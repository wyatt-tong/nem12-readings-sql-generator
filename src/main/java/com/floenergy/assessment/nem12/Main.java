package com.floenergy.assessment.nem12;

import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    private static final Path DEFAULT_INPUT_PATH =
            Path.of("src", "main", "resources", "data", "nem12_sample_data.csv");

    private Main() {
    }

    /**
     * Runs the generator against the bundled sample NEM12 file.
     */
    public static void main(String[] args) throws IOException {
        new Nem12SqlGenerator().parseData(DEFAULT_INPUT_PATH.toString());
    }
}
