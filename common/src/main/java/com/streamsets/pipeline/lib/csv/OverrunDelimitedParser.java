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
package com.streamsets.pipeline.lib.csv;

import com.streamsets.pipeline.api.ext.io.OverrunReader;
import com.univocity.parsers.csv.CsvParserSettings;

import java.io.IOException;
import java.io.Reader;

public class OverrunDelimitedParser extends DelimitedParser {
  private boolean overrun;

  public OverrunDelimitedParser(Reader reader, CsvParserSettings settings, int maxObjectLen) throws IOException {
    this(reader, settings, 0, maxObjectLen);
  }

  public OverrunDelimitedParser(Reader reader, CsvParserSettings settings, long initialPosition, int maxObjectLen) throws IOException {
    this(
        new OverrunReader(reader, OverrunReader.getDefaultReadLimit(), false, false),
        settings,
        initialPosition,
        maxObjectLen
    );
  }

  public OverrunDelimitedParser(
      OverrunReader reader,
      CsvParserSettings settings,
      long initialPosition,
      int maxObjectLen
  ) throws IOException {
    super(reader, settings, maxObjectLen, initialPosition);
    OverrunReader countingReader = (OverrunReader) getReader();
    countingReader.setEnabled(true);
  }
}
