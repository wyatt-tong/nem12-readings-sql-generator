package com.floenergy.assessment.nem12;

import com.floenergy.assessment.nem12.handler.AbstractRecordHandler;
import com.floenergy.assessment.nem12.handler.IntervalDataHandler;
import com.floenergy.assessment.nem12.handler.NmiDataHandler;
import com.floenergy.assessment.nem12.parser.Nem12HierarchyValidator;
import com.floenergy.assessment.nem12.parser.ParsingContext;
import com.floenergy.assessment.nem12.parser.RecordIndicator;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Nem12SqlGenerator {
    private static final int RECORD_INDICATOR_INDEX = 0;
    private static final Path OUTPUT_DIRECTORY = Path.of("output", "sql");

    /**
     * Parses one NEM12 file and writes the generated SQL output to the default output location.
     */
    public void parseData(String path) throws IOException {
        Path nemFilePath = Path.of(path);
        Path outputPath = resolveOutputPath(nemFilePath);
        Path temporaryOutputPath = resolveTemporaryOutputPath(outputPath);

        try (ParsingContext parsingContext = new ParsingContext(nemFilePath, temporaryOutputPath)) {
            parseRecords(parsingContext);
        } catch (Exception exception) {
            cleanupTemporaryOutputPath(temporaryOutputPath, exception);
            throw exception;
        }

        Files.move(temporaryOutputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Iterates through the parsed NEM12 records and routes each row by record type.
     */
    private void parseRecords(ParsingContext parsingContext) {
        CSVParser csvParser = parsingContext.getCsvParser();
        Nem12HierarchyValidator hierarchyValidator = new Nem12HierarchyValidator();

        for (CSVRecord record : csvParser) {
            RecordIndicator recordType = RecordIndicator.fromCode(record.get(RECORD_INDICATOR_INDEX));
            hierarchyValidator.accept(recordType, record);
            handleRecord(recordType, record, parsingContext);
        }

        hierarchyValidator.assertFinished();
    }

    /**
     * Dispatches one validated record to the handler that owns its parsing logic.
     */
    private void handleRecord(RecordIndicator recordType, CSVRecord record, ParsingContext parsingContext) {
        AbstractRecordHandler handler = null;
        switch (recordType) {
            case NMI_DATA_DETAILS -> handler = new NmiDataHandler(record, parsingContext);
            case INTERVAL_DATA -> handler = new IntervalDataHandler(record, parsingContext);
            case HEADER, B2B_DETAILS, END_OF_DATA -> {
                // do nothing for now
            }
        }
        if (handler != null) {
            handler.process();
        }
    }

    /**
     * Builds the default SQL output path by reusing the input file name and replacing its extension with {@code .sql}.
     */
    public static Path resolveOutputPath(Path nemFilePath) {
        if (nemFilePath == null) {
            throw new IllegalArgumentException("nemFilePath must not be null");
        }
        Path fileName = nemFilePath.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("nemFilePath must include a file name: " + nemFilePath);
        }

        String inputFileName = fileName.toString();
        int extensionSeparatorIndex = inputFileName.lastIndexOf('.');
        String baseName =
                extensionSeparatorIndex > 0 ? inputFileName.substring(0, extensionSeparatorIndex) : inputFileName;

        return OUTPUT_DIRECTORY.resolve(baseName + ".sql");
    }

    /**
     * Builds the temporary output path used while SQL generation is still in progress.
     */
    private Path resolveTemporaryOutputPath(Path outputPath) {
        Path fileName = outputPath.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("outputPath must include a file name: " + outputPath);
        }
        return outputPath.resolveSibling(fileName + ".tmp");
    }

    /**
     * Removes the temporary output file after a failed parse and preserves cleanup failures on the original exception.
     */
    private void cleanupTemporaryOutputPath(Path temporaryOutputPath, Exception originalException) {
        try {
            Files.deleteIfExists(temporaryOutputPath);
        } catch (IOException cleanupException) {
            originalException.addSuppressed(cleanupException);
        }
    }
}
