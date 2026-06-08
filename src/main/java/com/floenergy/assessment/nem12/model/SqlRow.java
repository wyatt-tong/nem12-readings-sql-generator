package com.floenergy.assessment.nem12.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Stores one SQL row as an ordered list of fields so renderers can preserve column order.
 */
public final class SqlRow {
    private final List<SqlField> fields = new ArrayList<>();

    /**
     * Appends one column/value pair to this row in insertion order.
     */
    public SqlRow addField(String column, Object value) {
        Objects.requireNonNull(column, "column must not be null");
        if (column.isBlank()) {
            throw new IllegalArgumentException("column must not be blank");
        }

        fields.add(new SqlField(column, value));
        return this;
    }

    /**
     * Creates a new row with the same ordered fields so callers can extend it independently.
     */
    public SqlRow copy() {
        SqlRow copiedRow = new SqlRow();
        copiedRow.fields.addAll(fields);
        return copiedRow;
    }

    /**
     * Returns an immutable snapshot of the fields currently stored in this row.
     */
    public List<SqlField> fields() {
        return List.copyOf(fields);
    }
}
