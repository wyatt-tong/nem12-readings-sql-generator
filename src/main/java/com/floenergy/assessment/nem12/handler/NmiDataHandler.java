package com.floenergy.assessment.nem12.handler;

import com.floenergy.assessment.nem12.model.SqlRow;
import com.floenergy.assessment.nem12.parser.ParsingContext;
import com.floenergy.assessment.nem12.parser.RecordIndicator;
import org.apache.commons.csv.CSVRecord;

/**
 * Handles one NEM12 200 record and prepares the shared base row for child 300 records.
 */
public class NmiDataHandler extends AbstractRecordHandler {
    public static final int NMI_POS = 1;
    public static final int INTERVAL_LENGTH_POS = 8;
    private final int intervalLength;

    /**
     * Creates a handler for one NEM12 200 record and validates the fields needed by downstream records.
     */
    public NmiDataHandler(CSVRecord record, ParsingContext context) {
        super(record, context);
        this.intervalLength = Integer.parseInt(record.get(INTERVAL_LENGTH_POS));
    }

    /**
     * Validates that the current record is a legal 200 record with a positive interval length.
     */
    @Override
    protected void validate() {
        if (RecordIndicator.fromCode(record.get(RECORD_INDICATOR_POS)) != RecordIndicator.NMI_DATA_DETAILS) {
            throw new IllegalArgumentException("Expected a 200 record but found: " + record.get(RECORD_INDICATOR_POS));
        }
        if (intervalLength <= 0) {
            throw new IllegalArgumentException("Illegal interval length of 200 record: " + intervalLength);
        }
    }

    /**
     * Copies the NMI from the 200 record into the shared base SQL row.
     */
    @Override
    protected void handleRecord() {
        context.setIntervalLength(this.intervalLength);
        SqlRow newSqlRow = new SqlRow();
        newSqlRow.addField("nmi", record.get(NMI_POS));
        context.setCurrentSql(newSqlRow);
    }
}
