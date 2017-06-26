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
package com.streamsets.pipeline.lib.csv;

import com.streamsets.pipeline.api.ext.io.CountingReader;
import com.streamsets.pipeline.api.impl.Utils;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.Closeable;
import java.io.Reader;

public class DelimitedParser implements Closeable, AutoCloseable {
  private long currentPos;
  private final CsvParser parser;
  private final CountingReader reader;

  public DelimitedParser(Reader reader, CsvParserSettings settings, int maxObjectLen) {
    this(new CountingReader(reader), settings, maxObjectLen, 0);
  }

  @SuppressWarnings("unchecked")
  public DelimitedParser(
      CountingReader reader,
      CsvParserSettings settings,
      int maxObjectLen,
      long initialPosition
  ) {
    Utils.checkNotNull(reader, "reader");
    Utils.checkNotNull(reader.getPos() == 0,
                       "reader must be in position zero, the DelimitedParser will fast-forward to the initialPosition");
    Utils.checkNotNull(settings, "format");
    Utils.checkArgument(initialPosition >= 0, "initialPosition must be greater or equal than zero");
    this.reader = reader;
    currentPos = initialPosition;
    parser = new CsvParser(settings);
    parser.beginParsing(reader);
  }

  protected Reader getReader() {
    return reader;
  }

  public String[] getHeaders() {
    return parser.getContext().headers();
  }

  public long getReaderPosition() {
    return currentPos;
  }

  @Override
  public void close() {
    parser.stopParsing();
  }
}
