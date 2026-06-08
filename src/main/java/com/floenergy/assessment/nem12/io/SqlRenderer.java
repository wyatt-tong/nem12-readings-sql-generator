package com.floenergy.assessment.nem12.io;

import com.floenergy.assessment.nem12.model.SqlField;
import com.floenergy.assessment.nem12.model.SqlRow;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Writes {@code meter_readings} insert statements to a target SQL file one row at a time.
 */
public class SqlRenderer implements AutoCloseable {
    private static final String TABLE_NAME = "meter_readings";
    private static final DateTimeFormatter SQL_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BufferedWriter writer;

    /**
     * Opens the target SQL file and prepares a buffered writer for streaming insert statements to disk.
     */
    public SqlRenderer(Path outputPath) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath must not be null");

        Path parentDirectory = outputPath.getParent();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }

        this.writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8);
    }

    /**
     * Renders one SQL row as an insert statement and appends it to the output file.
     */
    public void writeRow(SqlRow sqlRow) throws IOException {
        Objects.requireNonNull(sqlRow, "sqlRow must not be null");

        writer.write(renderInsert(sqlRow));
        writer.newLine();
    }

    /**
     * Closes the underlying writer and releases the file handle.
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }

    /**
     * Builds the complete insert statement for one logical SQL row.
     */
    private String renderInsert(SqlRow sqlRow) {
        List<SqlField> fields = sqlRow.fields();
        if (fields.isEmpty()) {
            throw new IllegalArgumentException("sqlRow must contain at least one field");
        }

        StringJoiner columnJoiner = new StringJoiner(", ");
        StringJoiner valueJoiner = new StringJoiner(", ");

        for (SqlField field : fields) {
            columnJoiner.add(quoteIdentifier(field.column()));
            valueJoiner.add(renderValue(field.value()));
        }

        return "insert into " + TABLE_NAME + " (" + columnJoiner + ") values (" + valueJoiner + ");";
    }

    /**
     * Renders one Java value into the SQL literal form expected by the target table.
     */
    private String renderValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof LocalDateTime localDateTime) {
            return "timestamp '" + localDateTime.format(SQL_TIMESTAMP_FORMATTER) + "'";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "'" + escapeSqlLiteral(value.toString()) + "'";
    }

    /**
     * Quotes one SQL identifier so reserved words such as {@code timestamp} remain valid.
     */
    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /**
     * Escapes a string literal so it can be written safely into the SQL output file.
     */
    private String escapeSqlLiteral(String value) {
        return value.replace("'", "''");
    }
}
