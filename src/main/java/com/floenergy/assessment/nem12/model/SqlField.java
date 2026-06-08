package com.floenergy.assessment.nem12.model;

/**
 * Represents one ordered column/value pair in an SQL insert row.
 */
public record SqlField(String column, Object value) {
}
