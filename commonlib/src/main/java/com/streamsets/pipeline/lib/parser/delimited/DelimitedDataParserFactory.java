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

import com.google.common.collect.ImmutableSet;
import com.streamsets.pipeline.api.ext.io.OverrunReader;
import com.streamsets.pipeline.api.impl.Utils;
import com.streamsets.pipeline.config.CsvHeader;
import com.streamsets.pipeline.config.CsvMode;
import com.streamsets.pipeline.config.CsvRecordType;
import com.streamsets.pipeline.lib.parser.DataParser;
import com.streamsets.pipeline.lib.parser.DataParserException;
import com.streamsets.pipeline.lib.parser.DataParserFactory;
import com.streamsets.pipeline.lib.util.DelimitedDataConstants;
import com.univocity.parsers.csv.CsvFormat;
import com.univocity.parsers.csv.CsvParserSettings;
import org.apache.commons.csv.CsvParserSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DelimitedDataParserFactory extends DataParserFactory {

  protected static final Map<String, Object> CONFIGS = new HashMap<>();
  static {
    CONFIGS.put(DelimitedDataConstants.DELIMITER_CONFIG, '|');
    CONFIGS.put(DelimitedDataConstants.ESCAPE_CONFIG, '\\');
    CONFIGS.put(DelimitedDataConstants.QUOTE_CONFIG, '"');
    CONFIGS.put(DelimitedDataConstants.SKIP_START_LINES, 0);
    CONFIGS.put(DelimitedDataConstants.PARSE_NULL, false);
    CONFIGS.put(DelimitedDataConstants.NULL_CONSTANT, "\\\\N");
    CONFIGS.put(DelimitedDataConstants.COMMENT_ALLOWED_CONFIG, false);
    CONFIGS.put(DelimitedDataConstants.COMMENT_MARKER_CONFIG, '#');
    CONFIGS.put(DelimitedDataConstants.IGNORE_EMPTY_LINES_CONFIG, true);
  }

  public static final Set<Class<? extends Enum>> MODES =
      ImmutableSet.of((Class<? extends Enum>) CsvMode.class, CsvHeader.class, CsvRecordType.class);

  public DelimitedDataParserFactory(Settings settings) {
    super(settings);
  }

  @Override
  public DataParser getParser(String id, InputStream is, String offset) throws DataParserException {
    return createParser(id, createReader(is), Long.parseLong(offset));
  }

  @Override
  public DataParser getParser(String id, Reader reader, long offset) throws DataParserException {
    return createParser(id, createReader(reader), offset);
  }

  private DataParser createParser(String id, OverrunReader reader, long offset) throws DataParserException {
    Utils.checkState(reader.getPos() == 0, Utils.formatL("reader must be in position '0', it is at '{}'",
                                                         reader.getPos()));
    CsvParserSettings parserSettings  = new CsvParserSettings();
    if (getSettings().getMode(CsvMode.class) == CsvMode.CUSTOM) {
      CsvFormat format = parserSettings .getFormat();
      format.setDelimiter(getSettings().getConfig(DelimitedDataConstants.DELIMITER_CONFIG));
      format.setQuoteEscape(getSettings().getConfig(DelimitedDataConstants.ESCAPE_CONFIG));
      format.setQuote(getSettings().getConfig(DelimitedDataConstants.QUOTE_CONFIG));
      parserSettings .setSkipEmptyLines(getSettings().getConfig(DelimitedDataConstants.IGNORE_EMPTY_LINES_CONFIG));
      if(getSettings().getConfig(DelimitedDataConstants.COMMENT_ALLOWED_CONFIG)) {
        format.setComment(getSettings().getConfig(DelimitedDataConstants.COMMENT_MARKER_CONFIG));
        parserSettings.setCommentCollectionEnabled(true);
      }
    }

    long skipLines = Long.parseLong(getSettings().getConfig(DelimitedDataConstants.SKIP_START_LINES));

    CsvHeader headerMode = getSettings().getMode(CsvHeader.class);
    parserSettings.setHeaderExtractionEnabled(false);
    
    if (CsvHeader.WITH_HEADER.equals(headerMode) && offset == 0) {
      parserSettings.setHeaderExtractionEnabled(true);
    } else if (CsvHeader.IGNORE_HEADER.equals(headerMode)) {
      parserSettings.setNumberOfRowsToSkip(++skipLines);
    }

    try {
      return new DelimitedCharDataParser(
        getSettings().getContext(),
        id,
        reader,
        offset,
        parserSettings,
        getSettings().getMaxRecordLen(),
        getSettings().getMode(CsvRecordType.class),
        (Boolean) getSettings().getConfig(DelimitedDataConstants.PARSE_NULL),
        (String) getSettings().getConfig(DelimitedDataConstants.NULL_CONSTANT)
      );
    } catch (IOException ex) {
      throw new DataParserException(Errors.DELIMITED_PARSER_00, id, offset, ex.toString(), ex);
    }
  }

}
