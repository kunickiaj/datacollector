/*
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.origin.tcp;

import com.google.common.collect.ImmutableSet;
import com.streamsets.pipeline.api.BatchMaker;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseSource;
import com.streamsets.pipeline.api.base.OnRecordErrorException;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.config.DatagramMode;
import com.streamsets.pipeline.lib.parser.tcp.AbstractParser;
import com.streamsets.pipeline.lib.parser.udp.ParserConfig;
import com.streamsets.pipeline.lib.parser.tcp.syslog.SyslogParser;
import com.streamsets.pipeline.lib.tcp.TCPConsumingServer;
import com.streamsets.pipeline.stage.common.DefaultErrorRecordHandler;
import com.streamsets.pipeline.stage.common.ErrorRecordHandler;
import com.streamsets.pipeline.stage.origin.udp.ParseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

import static com.streamsets.pipeline.lib.parser.udp.ParserConfigKey.CHARSET;


public class TCPSource extends BaseSource {
  private static final Logger LOG = LoggerFactory.getLogger(TCPSource.class);
  private static final boolean IS_TRACE_ENABLED = LOG.isTraceEnabled();
  private static final boolean IS_DEBUG_ENABLED = LOG.isDebugEnabled();
  private final int numThreads;
  private final Set<String> ports;
  private final int maxBatchSize;
  private final Queue<Record> overrunQueue;
  private final long maxWaitTime;
  private final List<InetSocketAddress> addresses;
  private final ParserConfig parserConfig;
  private final DatagramMode dataFormat;
  private long recordCount;
  private TCPConsumingServer tcpServer;
  private AbstractParser parser;
  private ErrorRecordHandler errorRecordHandler;
  private TransferQueue<ParseResult> incomingQueue;
  private boolean privilegedPortUsage = false;

  public TCPSource(
      List<String> ports,
      int numThreads,
      ParserConfig parserConfig,
      DatagramMode dataFormat,
      int maxBatchSize,
      long maxWaitTime
  ) {
    this.numThreads = numThreads;
    this.ports = ImmutableSet.copyOf(ports);
    this.parserConfig = parserConfig;
    this.dataFormat = dataFormat;
    this.maxBatchSize = maxBatchSize;
    this.maxWaitTime = maxWaitTime;
    this.overrunQueue = new LinkedList<>();
    this.addresses = new ArrayList<>();
  }

  @Override
  protected List<ConfigIssue> init() {
    List<ConfigIssue> issues = new ArrayList<>();
    errorRecordHandler = new DefaultErrorRecordHandler(getContext());

    this.recordCount = 0;
    this.incomingQueue = new LinkedTransferQueue<>();

    if (ports.isEmpty()) {
      issues.add(getContext().createConfigIssue(Groups.TCP.name(), "ports", Errors.TCP_02));
    } else {
      for (String candidatePort : ports) {
        try {
          int port = Integer.parseInt(candidatePort.trim());
          if (port > 0 && port < 65536) {
            if (port < 1024) {
              privilegedPortUsage = true; // only for error handling purposes
            }
            addresses.add(new InetSocketAddress(port));
          } else {
            issues.add(getContext().createConfigIssue(Groups.TCP.name(), "ports", Errors.TCP_03, port));
          }
        } catch (NumberFormatException ex) {
          issues.add(getContext().createConfigIssue(Groups.TCP.name(), "ports", Errors.TCP_03, candidatePort));
        }
      }
    }

    Charset charset;
    switch (dataFormat) {
      case SYSLOG:
        charset = validateCharset(Groups.SYSLOG.name(), issues);
        parser = new SyslogParser(getContext(), charset);
        break;
      default:
        issues.add(getContext().createConfigIssue(Groups.TCP.name(), "dataFormat", Errors.TCP_01, dataFormat));
        break;
    }
    if (issues.isEmpty()) {
      if (!addresses.isEmpty()) {
        QueuingTCPConsumer tcpConsumer = new QueuingTCPConsumer(parser, incomingQueue);
        tcpServer = new TCPConsumingServer(numThreads, addresses, tcpConsumer);
        try {
          tcpServer.listen();
          tcpServer.start();
        } catch (Exception ex) {
          tcpServer.destroy();
          tcpServer = null;

          if (ex instanceof SocketException && privilegedPortUsage) {
            issues.add(getContext().createConfigIssue(Groups.TCP.name(), "ports", Errors.TCP_07, ports, ex));
          } else {
            LOG.debug("Caught exception while starting up UDP server: {}", ex);
            issues.add(getContext().createConfigIssue(
                null,
                null,
                Errors.TCP_00,
                addresses.toString(),
                ex.toString(),
                ex
            ));
          }
        }
      }
    }
    return issues;
  }

  private Charset validateCharset(String groupName, List<ConfigIssue> issues) {
    Charset charset;
    try {
      charset = Charset.forName(parserConfig.getString(CHARSET));
    } catch (UnsupportedCharsetException ex) {
      charset = StandardCharsets.UTF_8;
      issues.add(getContext().createConfigIssue(groupName, "charset", Errors.TCP_04, charset));
    }
    return charset;
  }

  @Override
  public void destroy() {
    if (tcpServer != null) {
      tcpServer.destroy();
      tcpServer = null;
    }
    super.destroy();
  }

  @Override
  public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws StageException {
    Utils.checkNotNull(tcpServer, "UDP server is null");
    Utils.checkNotNull(incomingQueue, "Incoming queue is null");
    maxBatchSize = Math.min(this.maxBatchSize, maxBatchSize);
    final long startingRecordCount = recordCount;
    long remainingTime = maxWaitTime;
    for (int i = 0; i < maxBatchSize; i++) {
      if (overrunQueue.isEmpty()) {
        try {
          long start = System.currentTimeMillis();
          ParseResult result = incomingQueue.poll(remainingTime, TimeUnit.MILLISECONDS);
          long elapsedTime = System.currentTimeMillis() - start;
          if (elapsedTime > 0) {
            remainingTime -= elapsedTime;
          }
          if (result != null) {
            try {
              List<Record> records = result.getRecords();
              if (IS_TRACE_ENABLED) {
                LOG.trace("Found {} records", records.size());
              }
              overrunQueue.addAll(records);
            } catch (OnRecordErrorException ex) {
              errorRecordHandler.onError(ex.getErrorCode(), ex.getParams());
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      Record record = overrunQueue.poll();
      if (record != null) {
        recordCount++;
        batchMaker.addRecord(record);
      }
      if (remainingTime <= 0) {
        break;
      }
    }
    if (IS_DEBUG_ENABLED) {
      LOG.debug("Processed {} records", (recordCount - startingRecordCount));
    }
    return getOffset();
  }

  private String getOffset() {
    return Long.toString(recordCount);
  }
}
