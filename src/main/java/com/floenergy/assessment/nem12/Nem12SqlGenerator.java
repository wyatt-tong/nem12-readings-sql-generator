package com.floenergy.assessment.nem12;

import java.io.IOException;
import java.nio.file.Path;

public class Nem12SqlGenerator {
    private static final Path OUTPUT_DIRECTORY = Path.of("output", "sql");

    /**
     * Parses one NEM12 file and writes the generated SQL output to the default output location.
     */
    public void parseData(String path) throws IOException {

    }


    /**
     * Builds the default SQL output path by reusing the input file name and replacing its extension with {@code .sql}.
     */
    public static Path resolveOutputPath(Path nemFilePath) {
        if (nemFilePath == null) {
            throw new IllegalArgumentException("nemFilePath must not be null");
        }
        Path fileName = nemFilePath.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("nemFilePath must include a file name: " + nemFilePath);
        }

        String inputFileName = fileName.toString();
        int extensionSeparatorIndex = inputFileName.lastIndexOf('.');
        String baseName =
                extensionSeparatorIndex > 0 ? inputFileName.substring(0, extensionSeparatorIndex) : inputFileName;

        return OUTPUT_DIRECTORY.resolve(baseName + ".sql");
    }
}
