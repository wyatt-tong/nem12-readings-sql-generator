package com.floenergy.assessment.nem12.parser;

import com.floenergy.assessment.nem12.io.SqlRenderer;
import com.floenergy.assessment.nem12.model.SqlRow;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Stores shared parser state that must survive across multiple NEM12 records.
 */
public class ParsingContext implements AutoCloseable {
    private final CSVParser csvParser;
    private final SqlRenderer sqlRenderer;
    private SqlRow currentSql;
    private Integer intervalLength;

    /**
     * Creates an empty parsing context before any 200 record has populated shared state.
     */
    public ParsingContext(Path nemFilePath, Path outputPath) throws IOException {
        csvParser = openCsvParser(nemFilePath);
        sqlRenderer = new SqlRenderer(outputPath);
    }

    /**
     * Stores the current interval length in minutes for downstream 300 records.
     */
    public void setIntervalLength(int intervalLength) {
        if (intervalLength <= 0) {
            throw new IllegalArgumentException("intervalLength must be positive: " + intervalLength);
        }
        this.intervalLength = intervalLength;
    }

    /**
     * Returns the current interval length in minutes.
     */
    public int getIntervalLength() {
        if (intervalLength == null) {
            throw new IllegalStateException("intervalLength has not been set");
        }
        return intervalLength;
    }

    public SqlRow getCurrentSql() {
        if(currentSql == null) {
            currentSql = new SqlRow();
        }
        return currentSql;
    }

    public void setCurrentSql(SqlRow currentSql) {
        this.currentSql = Objects.requireNonNull(currentSql, "currentSql must not be null");
    }

    /**
     * Opens the input CSV file and returns a parser that streams records from disk.
     */
    private CSVParser openCsvParser(Path path) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setTrim(false)
                .setIgnoreEmptyLines(false)
                .get();

        Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        try {
            return csvFormat.parse(reader);
        } catch (IOException | RuntimeException exception) {
            reader.close();
            throw exception;
        }
    }

    public CSVParser getCsvParser() {
        return csvParser;
    }

    public SqlRenderer getSqlRenderer() {
        return sqlRenderer;
    }

    /**
     * Closes the parser and SQL renderer owned by this context, preserving both failures if cleanup is interrupted.
     */
    @Override
    public void close() throws IOException {
        IOException failure = null;

        try {
            csvParser.close();
        } catch (IOException e) {
            failure = e;
        }

        try {
            sqlRenderer.close();
        } catch (IOException e) {
            if (failure != null) {
                failure.addSuppressed(e);
            } else {
                failure = e;
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}
