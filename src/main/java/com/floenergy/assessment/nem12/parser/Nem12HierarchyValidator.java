package com.floenergy.assessment.nem12.parser;

import org.apache.commons.csv.CSVRecord;

/**
 * Validates that a flat NEM12 record stream follows the minimal parent-child ordering required by the parser.
 */
public class Nem12HierarchyValidator {
    private boolean headerSeen;
    private boolean currentNmiBlockOpen;
    private boolean endRecordSeen;

    /**
     * Validates one record against the current hierarchy state and advances the state if the record is accepted.
     */
    public void accept(RecordIndicator recordType, CSVRecord record) {
        assertValidHierarchy(recordType, record);
        advanceHierarchyState(recordType);
    }

    /**
     * Validates that the stream ended with the expected 900 end-of-data record.
     */
    public void assertFinished() {
        if (!endRecordSeen) {
            throw new IllegalArgumentException("Missing 900 end-of-data record");
        }
    }

    /**
     * Validates that the current record appears in a legal position within the flat NEM12 record stream.
     */
    private void assertValidHierarchy(RecordIndicator recordType, CSVRecord record) {
        if (endRecordSeen) {
            throw new IllegalArgumentException(
                    "Record found after 900 end-of-data at CSV line " + record.getRecordNumber());
        }

        switch (recordType) {
            case HEADER -> {
                if (headerSeen) {
                    throw new IllegalArgumentException(
                            "Duplicate 100 header record at CSV line " + record.getRecordNumber());
                }
            }
            case NMI_DATA_DETAILS -> {
                if (!headerSeen) {
                    throw new IllegalArgumentException(
                            "200 record found before 100 header at CSV line " + record.getRecordNumber());
                }
            }
            case INTERVAL_DATA, B2B_DETAILS -> {
                if (!headerSeen) {
                    throw new IllegalArgumentException(
                            recordType + " record found before 100 header at CSV line " + record.getRecordNumber());
                }
                if (!currentNmiBlockOpen) {
                    throw new IllegalArgumentException(
                            recordType + " record found without a preceding 200 record at CSV line "
                                    + record.getRecordNumber());
                }
            }
            case END_OF_DATA -> {
                if (!headerSeen) {
                    throw new IllegalArgumentException(
                            "900 record found before 100 header at CSV line " + record.getRecordNumber());
                }
            }
        }
    }

    /**
     * Advances the minimal hierarchy state after one record has been accepted by the validator.
     */
    private void advanceHierarchyState(RecordIndicator recordType) {
        switch (recordType) {
            case HEADER -> headerSeen = true;
            case NMI_DATA_DETAILS -> currentNmiBlockOpen = true;
            case INTERVAL_DATA, B2B_DETAILS -> {
                // no additional state to update
            }
            case END_OF_DATA -> endRecordSeen = true;
        }
    }
}
