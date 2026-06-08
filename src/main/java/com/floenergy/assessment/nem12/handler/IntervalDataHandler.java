package com.floenergy.assessment.nem12.handler;

import com.floenergy.assessment.nem12.model.SqlRow;
import com.floenergy.assessment.nem12.parser.ParsingContext;
import com.floenergy.assessment.nem12.parser.RecordIndicator;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class IntervalDataHandler extends AbstractRecordHandler {
    public static final int INTERVAL_DATE_POS = 1;
    public static final int FIRST_INTERVAL_VALUE_POS = 2;
    public static final int TRAILING_METADATA_FIELD_COUNT = 6;

    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final DateTimeFormatter INTERVAL_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final int consumptionLength;

    private int cursor;

    private final SqlRow baseSqlRow;

    private final LocalDateTime startTime;

    /**
     * Creates a handler for one NEM12 300 record and prepares timestamp generation for its intervals.
     */
    public IntervalDataHandler(CSVRecord record, ParsingContext context) {
        super(record, context);
        consumptionLength = record.size() - TRAILING_METADATA_FIELD_COUNT - FIRST_INTERVAL_VALUE_POS + 1;
        this.cursor = FIRST_INTERVAL_VALUE_POS;
        this.baseSqlRow = context.getCurrentSql();
        this.startTime = convertToDateTime(record.get(INTERVAL_DATE_POS));
    }

    /**
     * Validates that the current record is a legal 300 record whose interval count matches the active interval length.
     */
    @Override
    protected void validate() {
        if (RecordIndicator.fromCode(record.get(RECORD_INDICATOR_POS)) != RecordIndicator.INTERVAL_DATA) {
            throw new IllegalArgumentException("Expected a 300 record but found: " + record.get(RECORD_INDICATOR_POS));
        }
        if (consumptionLength <= 0) {
            throw new IllegalArgumentException("Illegal consumption length of 300 record: " + consumptionLength);
        }
        if (context.getIntervalLength() <= 0) {
            throw new IllegalArgumentException("Interval length in minutes must be positive: " + context.getIntervalLength());
        }
        int expectedConsumptionLength = expectedConsumptionLength();
        if (consumptionLength != expectedConsumptionLength) {
            throw new IllegalArgumentException(
                    "300 record interval count " + consumptionLength
                            + " does not match interval length " + context.getIntervalLength()
                            + " minutes; expected " + expectedConsumptionLength);
        }
        if (record.size() < FIRST_INTERVAL_VALUE_POS + consumptionLength) {
            throw new IllegalArgumentException(
                    "300 record does not contain the expected number of interval values: " + consumptionLength);
        }
    }

    /**
     * Writes one SQL row for each interval value carried by the current 300 record.
     */
    @Override
    protected void handleRecord() {
        while (hasNextConsumption()) {
            try {
                context.getSqlRenderer().writeRow(nextSqlRow());
            } catch (IOException e) {
                throw new UncheckedIOException(
                        "Failed to write SQL row for 300 record at CSV line " + record.getRecordNumber(), e);
            }
        }
    }

    /**
     * Builds the next SQL row for one interval reading using the shared base row from the parent 200 record.
     */
    public SqlRow nextSqlRow() {
        if (hasNextConsumption()) {
            SqlRow intervalSqlRow = baseSqlRow.copy();
            intervalSqlRow.addField("timestamp", nextTimestamp())
                    .addField("consumption", nextConsumption());
            return intervalSqlRow;
        }
        return null;
    }

    /**
     * Returns whether the current 300 record still has unread interval values.
     */
    public boolean hasNextConsumption() {
        return cursor < consumptionLength + FIRST_INTERVAL_VALUE_POS;
    }

    /**
     * Returns the next consumption value and advances the interval cursor.
     */
    public BigDecimal nextConsumption() {
        if (hasNextConsumption()) {
            return new BigDecimal(record.get(cursor++));
        }
        return null;
    }

    /**
     * Converts the NEM12 interval date field into the midnight baseline used for interval offsets.
     */
    private LocalDateTime convertToDateTime(String date) {
        return LocalDate.parse(date, INTERVAL_DATE_FORMATTER).atStartOfDay();
    }

    /**
     * Computes the next end-of-interval timestamp as a LocalDateTime value.
     */
    private LocalDateTime nextTimestamp() {
        long intervalNumber = cursor - FIRST_INTERVAL_VALUE_POS + 1L;
        return startTime.plusMinutes(context.getIntervalLength() * intervalNumber);
    }

    /**
     * Computes the number of interval values a full-day 300 record must contain for the current interval length.
     */
    private int expectedConsumptionLength() {
        int intervalLength = context.getIntervalLength();
        if (MINUTES_PER_DAY % intervalLength != 0) {
            throw new IllegalArgumentException(
                    "Interval length must divide evenly into one day: " + intervalLength);
        }
        return MINUTES_PER_DAY / intervalLength;
    }
}
