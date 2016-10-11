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

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.Source;
import com.streamsets.pipeline.api.StageDef;
import com.streamsets.pipeline.api.ValueChooserModel;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.config.CharsetChooserValues;
import com.streamsets.pipeline.config.DatagramMode;
import com.streamsets.pipeline.config.DatagramModeChooserValues;
import com.streamsets.pipeline.configurablestage.DSource;
import com.streamsets.pipeline.lib.parser.udp.ParserConfig;
import com.streamsets.pipeline.stage.origin.udp.UDPSourceUpgrader;

import java.util.List;

import static com.streamsets.pipeline.lib.parser.udp.ParserConfigKey.CHARSET;

@StageDef(
    version = 1,
    label = "TCP Source",
    description = "Listens for TCP messages",
    icon = "udp.png",
    execution = ExecutionMode.STANDALONE,
    recordsByRef = true,
    upgrader = UDPSourceUpgrader.class,
    onlineHelpRefUrl = "TODO"
)

@ConfigGroups(Groups.class)
@GenerateResourceBundle
public class TCPDSource extends DSource {
  private ParserConfig parserConfig = new ParserConfig();

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.LIST,
      label = "Port",
      defaultValue = "[\"9995\"]",
      description = "Port to listen on",
      group = "TCP",
      displayPosition = 10
  )
  public List<String> ports; // string so we can listen on multiple ports in the future

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.NUMBER,
      label = "Number of Receive Threads",
      defaultValue = "1",
      group = "TCP",
      displayPosition = 15
  )
  public int numThreads;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      label = "Data Format",
      defaultValue = "SYSLOG",
      group = "TCP",
      displayPosition = 20
  )
  @ValueChooserModel(DatagramModeChooserValues.class)
  public DatagramMode dataFormat;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.NUMBER,
      defaultValue = "1000",
      label = "Max Batch Size (messages)",
      group = "TCP",
      displayPosition = 30,
      min = 0,
      max = Integer.MAX_VALUE
  )
  public int batchSize;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.NUMBER,
      defaultValue = "1000",
      label = "Batch Wait Time (ms)",
      description = "Max time to wait for data before sending a batch",
      displayPosition = 40,
      group = "TCP",
      min = 1,
      max = Integer.MAX_VALUE
  )
  public int maxWaitTime;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "UTF-8",
      label = "Charset",
      displayPosition = 5,
      group = "SYSLOG",
      dependsOn = "dataFormat",
      triggeredByValue = "SYSLOG"
  )
  @ValueChooserModel(CharsetChooserValues.class)
  public String syslogCharset;


  @Override
  protected Source createSource() {
    Utils.checkNotNull(dataFormat, "Data format cannot be null");
    Utils.checkNotNull(ports, "Ports cannot be null");

    parserConfig.put(CHARSET, syslogCharset);

    return new TCPSource(ports, numThreads, parserConfig, dataFormat, batchSize, maxWaitTime);
  }
}
