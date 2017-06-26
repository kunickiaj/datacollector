/**
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
package com.streamsets.pipeline.lib.parser.delimited;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.Stage;
import com.streamsets.pipeline.api.ext.io.OverrunReader;
import com.streamsets.pipeline.config.CsvRecordType;
import com.streamsets.pipeline.lib.csv.OverrunDelimitedParser;
import com.streamsets.pipeline.lib.parser.AbstractDataParser;
import com.streamsets.pipeline.lib.parser.DataParserException;
import com.streamsets.pipeline.lib.parser.RecoverableDataParserException;
import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.processor.RowProcessor;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DelimitedCharDataParser extends AbstractDataParser {
  private final Stage.Context context;
  private final String readerId;
  private final OverrunDelimitedParser parser;
  private List<Field> headers;
  private boolean eof;
  private CsvRecordType recordType;
  private final String nullConstant;

  public DelimitedCharDataParser(
      Stage.Context context,
      String readerId,
      OverrunReader reader,
      long readerOffset,
      CsvParserSettings settings,
      int maxObjectLen,
      CsvRecordType recordType,
      boolean parseNull,
      String nullConstant
  )
    throws IOException {
    this.context = context;
    this.readerId = readerId;
    this.recordType = recordType;
    this.nullConstant = parseNull ? nullConstant : null;
    parser = new OverrunDelimitedParser(reader, settings, readerOffset, maxObjectLen);
  }

  @Override
  public Record parse() throws IOException, DataParserException {
    Record record = null;
    long offset = parser.getReaderPosition();
    String[] columns = parser.
    if (columns != null) {
      record = createRecord(offset, columns);
    } else {
      eof = true;
    }
    return record;
  }


  class DelimitedRowProcessor implements RowProcessor {

    private final Stage.Context stageContext;
    private Record currentRecord;

    public DelimitedRowProcessor(Stage.Context stageContext) {
      this.stageContext = stageContext;
    }

    public final Record getCurrentRecord() {
      return this.currentRecord;
    }

    @Override
    public void processStarted(ParsingContext context) {

    }

    @Override
    public void rowProcessed(String[] row, ParsingContext context) {
      Record record = stageContext.createRecord(readerId + "::" + context.currentChar());

      headers = Arrays.stream(context.headers()).map(Field::create).collect(Collectors.toList());
      if(recordType == CsvRecordType.LIST) {
        List<Field> root = new ArrayList<>();
        for (int i = 0; i < row.length; i++) {
          Map<String, Field> cell = new HashMap<>();
          Field header = headers.get(i);
          if (header != null) {
            cell.put("header", header);
          }
          Field value = getField(row[i]);
          cell.put("value", value);
          root.add(Field.create(cell));
        }
        record.set(Field.create(root));
      } else {
        LinkedHashMap<String, Field> listMap = new LinkedHashMap<>();
        for (int i = 0; i < row.length; i++) {
          String key;
          Field header = headers.get(i);
          if(header != null) {
            key = header.getValueAsString();
          } else {
            key = Integer.toString(i);
          }
          listMap.put(key, getField(row[i]));
        }
        record.set(Field.createListMap(listMap));
      }

      currentRecord = record;
    }

    @Override
    public void processEnded(ParsingContext context) {
    }

    private Field getField(String value) {
      if(nullConstant != null && nullConstant.equals(value)) {
        return Field.create(Field.Type.STRING, null);
      }

      return Field.create(Field.Type.STRING, value);
    }
  }

  private Field getListField(String ...values) {
    ImmutableList.Builder<Field> listBuilder = ImmutableList.builder();

    Arrays.stream(values).forEach(v -> listBuilder.add(Field.create(Field.Type.STRING, v)));

    return Field.create(Field.Type.LIST, listBuilder.build());
  }

  @Override
  public String getOffset() {
    return (eof) ? String.valueOf(-1) : String.valueOf(parser.getReaderPosition());
  }

  @Override
  public void close() throws IOException {
    parser.close();
  }

}
