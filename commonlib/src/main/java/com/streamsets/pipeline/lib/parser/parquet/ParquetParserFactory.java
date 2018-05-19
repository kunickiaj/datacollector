/*
 * Copyright 2018 StreamSets Inc.
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
package com.streamsets.pipeline.lib.parser.parquet;

import com.streamsets.pipeline.config.ExcelHeader;
import com.streamsets.pipeline.lib.parser.DataParser;
import com.streamsets.pipeline.lib.parser.DataParserException;
import com.streamsets.pipeline.lib.parser.DataParserFactory;
import com.streamsets.pipeline.lib.parser.excel.Errors;
import com.streamsets.pipeline.lib.parser.excel.WorkbookParser;
import com.streamsets.pipeline.lib.parser.excel.WorkbookParserSettings;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableInput;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.mapred.FsInput;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ParquetParserFactory extends DataParserFactory {
  public static final Map<String, Object> CONFIGS;
  public static final Set<Class<? extends Enum>> MODES;

  static {
    CONFIGS = Collections.emptyMap();
    MODES = new HashSet<>();
  }

  public ParquetParserFactory(Settings settings) {
    super(settings);
  }

  @Override
  public DataParser getParser(String id, InputStream is, String offset) throws DataParserException {
    return createParser(is, offset);
  }

  @NotNull
  private DataParser createParser(InputStream is, String offset) throws DataParserException {
    return new ParquetDataStreamParser();
    /*SeekableInput seekableInput = new FsInput(inputPath, conf);
    DatumReader<GenericRecord> reader = new GenericDatumReader<>();
    FileReader<GenericRecord> fileReader = DataFileReader.openReader(seekableInput, reader);
    Schema avroSchema = fileReader.getSchema();

    Workbook workbook = open(is);

    WorkbookParserSettings workbookSettings = WorkbookParserSettings.builder()
        .withHeader(getSettings().getMode(ExcelHeader.class))
        .build();
    return new WorkbookParser(workbookSettings, getSettings().getContext(), workbook, offset);*/
  }

  private Workbook open(InputStream is) throws DataParserException {
    try {
      return WorkbookFactory.create(is);
    } catch (IOException e) {
      throw new DataParserException(Errors.EXCEL_PARSER_01, e);
    } catch (InvalidFormatException e) {
      throw new DataParserException(Errors.EXCEL_PARSER_02, e);
    } catch (EncryptedDocumentException e) {
      throw new DataParserException(Errors.EXCEL_PARSER_03, e);
    }
  }

  @Override
  public DataParser getParser(String id, Reader reader, long offset) throws DataParserException {
    throw new DataParserException(Errors.EXCEL_PARSER_00);
  }
}