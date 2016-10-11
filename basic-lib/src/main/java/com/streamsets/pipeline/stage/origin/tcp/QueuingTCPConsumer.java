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

import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.base.OnRecordErrorException;
import com.streamsets.pipeline.lib.parser.tcp.AbstractParser;
import com.streamsets.pipeline.lib.tcp.TCPConsumer;
import com.streamsets.pipeline.stage.origin.udp.ParseResult;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicLong;

public class QueuingTCPConsumer implements TCPConsumer {
  private static final Logger LOG = LoggerFactory.getLogger(QueuingTCPConsumer.class);

  private final AbstractParser parser;
  private final TransferQueue<ParseResult> queue;
  private final AtomicLong totalPackets;

  public QueuingTCPConsumer(AbstractParser parser, TransferQueue<ParseResult> queue) {
    this.parser = parser;
    this.queue = queue;
    this.totalPackets = new AtomicLong(0);
  }

  @Override
  public void process(ByteBuf msg) throws Exception {
    totalPackets.incrementAndGet();
    ParseResult result;
    try {
      List<Record> records = parser.parse(msg);
      result = new ParseResult(records);
    } catch (OnRecordErrorException ex) {
      result = new ParseResult(ex);
    }
    queue.transfer(result);
  }
}
