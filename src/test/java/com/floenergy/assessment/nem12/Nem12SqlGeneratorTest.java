package com.floenergy.assessment.nem12;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Nem12SqlGeneratorTest {
    @TempDir
    Path tempDir;

    @Test
    void testThirtyMinuteIntervals() throws IOException {
        Path inputPath = writeInputFile(
                "interval-thirty-minute.csv",
                String.join("\n",
                        "100,NEM12,200506081149,UNITEDDP,NEMMCO",
                        "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610",
                        "300,20050301,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,A,,,20050310121004,20050310182204",
                        "900"));
        Path outputPath = Nem12SqlGenerator.resolveOutputPath(inputPath);

        try {
            new Nem12SqlGenerator().parseData(inputPath.toString());

            List<String> sqlLines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);
            assertEquals(48, sqlLines.size());
            assertEquals(
                    "insert into meter_readings (\"nmi\", \"timestamp\", \"consumption\") values ('NEM1201009', timestamp '2005-03-01 00:30:00', 1);",
                    sqlLines.getFirst());
            assertEquals(
                    "insert into meter_readings (\"nmi\", \"timestamp\", \"consumption\") values ('NEM1201009', timestamp '2005-03-02 00:00:00', 48);",
                    sqlLines.getLast());
        } finally {
            Files.deleteIfExists(outputPath);
        }
    }

    @Test
    void testSampleFileOutput() throws IOException {
        Path sampleInputPath = Path.of("src", "main", "resources", "data", "nem12_sample_data.csv");
        Path expectedOutputPath = Path.of("src", "test", "resources", "expected", "nem12_sample_data.sql");
        Path inputPath = tempDir.resolve("bundled-sample-copy.csv");
        Path outputPath = Nem12SqlGenerator.resolveOutputPath(inputPath);
        Files.writeString(inputPath, Files.readString(sampleInputPath, StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        try {
            new Nem12SqlGenerator().parseData(inputPath.toString());

            List<String> expectedSqlLines = Files.readAllLines(expectedOutputPath, StandardCharsets.UTF_8);
            List<String> actualSqlLines = Files.readAllLines(outputPath, StandardCharsets.UTF_8);
            assertEquals(expectedSqlLines, actualSqlLines);
        } finally {
            Files.deleteIfExists(outputPath);
        }
    }

    @Test
    void testMissingParent200() throws IOException {
        Path inputPath = writeInputFile(
                "missing-parent-200.csv",
                String.join("\n",
                        "100,NEM12,200506081149,UNITEDDP,NEMMCO",
                        "300,20050301,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,A,,,20050310121004,20050310182204",
                        "900"));
        Path outputPath = Nem12SqlGenerator.resolveOutputPath(inputPath);

        try {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Nem12SqlGenerator().parseData(inputPath.toString()));
            assertEquals(
                    "INTERVAL_DATA record found without a preceding 200 record at CSV line 2",
                    exception.getMessage());
        } finally {
            Files.deleteIfExists(outputPath);
        }
    }

    /**
     * Verifies that the parser reports a hierarchy error when additional records appear after the 900 end marker.
     */
    @Test
    void testRecordAfter900() throws IOException {
        Path inputPath = writeInputFile(
                "record-after-900.csv",
                String.join("\n",
                        "100,NEM12,200506081149,UNITEDDP,NEMMCO",
                        "200,NEM1201009,E1E2,1,E1,N1,01009,kWh,30,20050610",
                        "900",
                        "300,20050301,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,A,,,20050310121004,20050310182204"));
        Path outputPath = Nem12SqlGenerator.resolveOutputPath(inputPath);

        try {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new Nem12SqlGenerator().parseData(inputPath.toString()));
            assertEquals(
                    "Record found after 900 end-of-data at CSV line 4",
                    exception.getMessage());
        } finally {
            Files.deleteIfExists(outputPath);
        }
    }

    /**
     * Writes one temporary NEM12 input file for the current test case and returns its path.
     */
    private Path writeInputFile(String fileName, String content) throws IOException {
        Path inputPath = tempDir.resolve(fileName);
        Files.writeString(inputPath, content, StandardCharsets.UTF_8);
        return inputPath;
    }
}
