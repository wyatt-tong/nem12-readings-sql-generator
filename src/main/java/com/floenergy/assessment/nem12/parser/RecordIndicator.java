package com.floenergy.assessment.nem12.parser;

public enum RecordIndicator {
    HEADER("100"),
    NMI_DATA_DETAILS("200"),
    INTERVAL_DATA("300"),
    B2B_DETAILS("500"),
    END_OF_DATA("900");

    private final String code;

    RecordIndicator(String code) {
        this.code = code;
    }

    public static RecordIndicator fromCode(String code) {
        for (RecordIndicator recordIndicator : values()) {
            if (recordIndicator.code.equals(code)) {
                return recordIndicator;
            }
        }
        throw new IllegalArgumentException("Unknown NEM12 record indicator: " + code);
    }
}
