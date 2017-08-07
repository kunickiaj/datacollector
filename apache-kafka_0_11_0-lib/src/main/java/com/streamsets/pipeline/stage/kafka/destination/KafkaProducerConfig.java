/*
 * Copyright 2017 StreamSets Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.kafka.destination;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigDefBean;
import com.streamsets.pipeline.api.ValueChooserModel;
import com.streamsets.pipeline.config.DataFormat;
import com.streamsets.pipeline.lib.el.RecordEL;
import com.streamsets.pipeline.stage.destination.lib.DataGeneratorFormatConfig;
import com.streamsets.pipeline.stage.kafka.lib.Serializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KafkaProducerConfig {

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "SDC_JSON",
      label = "Data Format",
      displayPosition = 1,
      group = "DATA_FORMAT"
  )
  @ValueChooserModel(ProducerDataFormatChooserValues.class)
  public DataFormat dataFormat;

  @ConfigDefBean
  public DataGeneratorFormatConfig dataFormatConfig = new DataGeneratorFormatConfig();

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.LIST,
      defaultValue = "localhost:9092",
      label = "Bootstrap Brokers",
      description = "Comma-separated list of URIs for brokers that write to the topic.  Use the format " +
          "<HOST>:<PORT>.",
      displayPosition = 10,
      group = "#0"
  )
  public List<String> brokers;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.BOOLEAN,
      defaultValue = "false",
      label = "Runtime Topic Resolution",
      description = "Select topic at runtime based on the field values in the record",
      displayPosition = 15,
      group = "#0"
  )
  public boolean runtimeTopicResolution;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      defaultValue = "${record:value('/topic')}",
      label = "Topic Expression",
      description = "An expression that resolves to the name of the topic to use",
      displayPosition = 20,
      elDefs = {RecordEL.class},
      group = "#0",
      evaluation = ConfigDef.Evaluation.EXPLICIT,
      dependsOn = "runtimeTopicResolution",
      triggeredByValue = "true"
  )
  public String topicExpression;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.TEXT,
      lines = 5,
      defaultValue = "*",
      label = "Topic White List",
      description = "A comma-separated list of valid topic names. " +
          "Records with invalid topic names are treated as error records. " +
          "'*' indicates that all topic names are allowed.",
      displayPosition = 23,
      group = "#0",
      dependsOn = "runtimeTopicResolution",
      triggeredByValue = "true"
  )
  public String topicWhiteList;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      defaultValue = "topicName",
      label = "Topic",
      description = "",
      displayPosition = 25,
      group = "#0",
      dependsOn = "runtimeTopicResolution",
      triggeredByValue = "false"
  )
  public String topic;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      defaultValue = "ROUND_ROBIN",
      label = "Partition Strategy",
      description = "Strategy to select a partition to write to",
      displayPosition = 30,
      group = "#0"
  )
  @ValueChooserModel(PartitionStrategyChooserValues.class)
  public PartitionStrategy partitionStrategy;

  @ConfigDef(
      required = false,
      type = ConfigDef.Type.STRING,
      defaultValue = "${0}",
      label = "Partition Expression",
      description = "When using the default partition strategy, enter an expression to evaluate the partition key " +
          "from record, which will be used with hash function to determine the topic's partition. " +
          "When using Expression, enter an expression that determines the partition number. ",
      displayPosition = 40,
      group = "#0",
      dependsOn = "partitionStrategy",
      triggeredByValue = {"EXPRESSION", "DEFAULT"},
      elDefs = {RecordEL.class},
      evaluation = ConfigDef.Evaluation.EXPLICIT
  )
  public String partition;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.BOOLEAN,
      defaultValue = "false",
      label = "One Message per Batch",
      description = "Generates a single Kafka message with all records in the batch",
      displayPosition = 50,
      group = "#0"
  )
  public boolean singleMessagePerBatch;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      label = "Key Serializer",
      description = "Method used to serialize the Kafka message key. Set to Confluent to embed the Avro schema ID in each message the destination writes.",
      defaultValue = "STRING",
      displayPosition = 440,
      dependsOn = "dataFormat",
      triggeredByValue = "AVRO",
      group = "KAFKA"
  )
  @ValueChooserModel(KeySerializerChooserValues.class)
  public Serializer keySerializer = Serializer.STRING;

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.MODEL,
      label = "Value Serializer",
      description = "Method used to serialize the Kafka message value. Set to Confluent to embed the Avro schema ID in each message the destination writes.",
      defaultValue = "DEFAULT",
      displayPosition = 450,
      dependsOn = "dataFormat",
      triggeredByValue = "AVRO",
      group = "KAFKA"
  )
  @ValueChooserModel(ValueSerializerChooserValues.class)
  public Serializer valueSerializer = Serializer.DEFAULT;

  @ConfigDef(
      required = false,
      type = ConfigDef.Type.MAP,
      defaultValue = "",
      label = "Kafka Configuration",
      description = "Additional Kafka properties to pass to the underlying Kafka producer",
      displayPosition = 60,
      group = "#0"
  )
  public Map<String, Object> producerConfigs = new HashMap<>();
}
