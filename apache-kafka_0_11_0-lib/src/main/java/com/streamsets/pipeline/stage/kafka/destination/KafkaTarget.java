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

import com.streamsets.pipeline.api.Batch;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseTarget;
import com.streamsets.pipeline.lib.generator.DataGenerator;
import com.streamsets.pipeline.lib.generator.DataGeneratorException;
import com.streamsets.pipeline.lib.generator.DataGeneratorFactory;
import com.streamsets.pipeline.stage.common.DefaultErrorRecordHandler;
import com.streamsets.pipeline.stage.common.ErrorRecordHandler;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class KafkaTarget extends BaseTarget {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaTarget.class);

  private final KafkaProducerConfig conf;

  private KafkaProducer<String, byte[]> producer;
  private DataGeneratorFactory generatorFactory;
  private ErrorRecordHandler errorRecordHandler;

  public KafkaTarget(KafkaProducerConfig conf) {
    this.conf = conf;
  }

  @Override
  protected List<ConfigIssue> init() {
    List<ConfigIssue> issues = super.init();

    errorRecordHandler = new DefaultErrorRecordHandler(getContext());

    if (conf.dataFormatConfig.init(
        getContext(),
        conf.dataFormat,
        Groups.DATA_FORMAT.name(),
        "conf.dataFormat.",
        issues
    )) {
      generatorFactory = conf.dataFormatConfig.getDataGeneratorFactory();
    }

    producer = new KafkaProducer<>(conf.producerConfigs);

    return issues;
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  @Override
  public void write(Batch batch) throws StageException {
    Iterator<Record> records = batch.getRecords();

    List<Future<RecordMetadata>> futures = new LinkedList<>();

    while (records.hasNext()) {
      Record record = records.next();
      futures.add(publish(record));
    }

    futures.forEach(f -> {
      try {
        f.get();
      } catch (ExecutionException e) {

      } catch (InterruptedException e) {
        // force stopped
        Thread.currentThread().interrupt();
      }
    });
  }

  private Future<RecordMetadata> publish(Record record) throws StageException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();
    try (DataGenerator generator = generatorFactory.getGenerator(os)) {
      generator.write(record);
    } catch (IOException | DataGeneratorException e) {
    }

    return producer.send(new ProducerRecord<>(conf.topic, os.toByteArray()));
  }
}
