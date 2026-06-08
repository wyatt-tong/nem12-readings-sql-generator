package com.floenergy.assessment.nem12.handler;

import com.floenergy.assessment.nem12.parser.ParsingContext;
import org.apache.commons.csv.CSVRecord;

import java.util.Objects;

public abstract class AbstractRecordHandler {
    public static final int RECORD_INDICATOR_POS = 0;
    protected final CSVRecord record;

    protected final ParsingContext context;

    /**
     * Stores the shared record and parsing context needed by concrete NEM12 handlers.
     */
    public AbstractRecordHandler(CSVRecord record, ParsingContext context) {
        Objects.requireNonNull(record, "record must not be null");
        Objects.requireNonNull(context, "parsing context must not be null");
        this.record = record;
        this.context = context;
    }

    /**
     * Validates that the current record and shared parser state are consistent for this handler type.
     */
    protected abstract void validate();

    /**
     * Validates the current handler state and then processes the record in one enforced lifecycle step.
     */
    public final void process() {
        validate();
        handleRecord();
    }

    /**
     * Processes the current record after validation has completed successfully.
     */
    protected abstract void handleRecord();
}
