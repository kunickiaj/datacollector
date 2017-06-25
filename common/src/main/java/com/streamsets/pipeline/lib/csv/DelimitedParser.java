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
import com.streamsets.pipeline.api.ext.io.ObjectLengthException;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.lib.util.ExceptionUtils;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class DelimitedParser implements Closeable, AutoCloseable {
  private long currentPos;
  private long skipLinesPosCorrection;
  private final CsvParser parser;
  private final CountingReader reader;
  private final int maxObjectLen;
  private Record nextRecord;
  private final String[] headers;
  private boolean closed;

  public DelimitedParser(Reader reader, CsvParserSettings settings, int maxObjectLen) throws IOException {
    this(new CountingReader(reader), settings, maxObjectLen, 0, 0);
  }

  @SuppressWarnings("unchecked")
  public DelimitedParser(
      CountingReader reader,
      CsvParserSettings settings,
      int maxObjectLen,
      long initialPosition
  ) throws IOException {
    Utils.checkNotNull(reader, "reader");
    Utils.checkNotNull(reader.getPos() == 0,
                       "reader must be in position zero, the DelimitedParser will fast-forward to the initialPosition");
    Utils.checkNotNull(settings, "format");
    Utils.checkArgument(initialPosition >= 0, "initialPosition must be greater or equal than zero");
    Utils.checkArgument(skipStartLines >= 0, "skipStartLines must be greater or equal than zero");
    this.reader = reader;
    currentPos = initialPosition;
    this.maxObjectLen = maxObjectLen;

    parser = new CsvParser(settings);
    long skipStartLines = settings.getNumberOfRowsToSkip();
    if (initialPosition == 0) {
      parser.beginParsing(reader);
      currentPos = parser.getContext().currentChar();
      headers = parser.parseNext();
    } else {
      if (format.getSkipHeaderRecord()) {
        format = format.withSkipHeaderRecord(false);
        parser = new CsvParser(reader, format, 0, 0);
        headers = read();
        while (getReaderPosition() < initialPosition && read() != null) {
        }
        if (getReaderPosition() != initialPosition) {
          throw new IOException(Utils.format("Could not position reader at position '{}', got '{}' instead",
                                             initialPosition, getReaderPosition()));
        }
      } else {
        IOUtils.skipFully(reader, initialPosition);
        parser = new CsvParser(reader, format, initialPosition, 0);
        headers = null;
      }
    }
  }

  protected Reader getReader() {
    return reader;
  }

  protected Record nextRecord() {
    return (iterator.hasNext()) ? iterator.next() : null;
  }

  public String[] getHeaders() {
    return headers;
  }

  public long getReaderPosition() {
    return currentPos;
  }

  public String[] read() throws IOException {
    if (closed) {
      throw new IOException("Parser has been closed");
    }
    if (iterator == null) {
      iterator = parser.iterator();
      nextRecord = nextRecord();
    }
    Record record = nextRecord;
    if (nextRecord != null) {
      nextRecord = nextRecord();
    }
    long prevPos = currentPos;
    currentPos = (nextRecord != null) ? nextRecord.getCharacterPosition() + skipLinesPosCorrection : reader.getPos();
    if (maxObjectLen > -1) {
      if (currentPos - prevPos > maxObjectLen) {
        ExceptionUtils.throwUndeclared(new ObjectLengthException(Utils.format(
            "CSV Object at offset '{}' exceeds max length '{}'", prevPos, maxObjectLen), prevPos));
      }
    }
    return toArray(record);
  }

  private String[] toArray(Record record) {
    String[] array = (record == null) ? null : new String[record.size()];
    if (array != null) {
      for (int i = 0; i < record.size(); i++) {
        array[i] = record.get(i);
      }
    }
    return array;
  }

  @Override
  public void close() {
    closed = true;
    parser.stopParsing();
  }
}
