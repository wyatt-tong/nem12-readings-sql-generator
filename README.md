# NEM12 Readings SQL Generator

This project parses a NEM12 meter data file and generates PostgreSQL `INSERT` statements for a `meter_readings` table.

The implementation is designed as a streaming pipeline:
- input is read record by record
- `200` records establish the current NMI context
- `300` records are expanded into one SQL row per interval reading
- output is written to disk as SQL one row at a time

This keeps memory usage bounded and makes the solution suitable for large files.

## Tech Stack

- Java 21
- Maven
- Apache Commons CSV
- JUnit 5

## Project Structure

```text
src/main/java/com/floenergy/assessment/nem12
├── Main.java                         demo entry point for the bundled sample file
├── Nem12SqlGenerator.java            top-level orchestration
├── handler
│   ├── AbstractRecordHandler.java
│   ├── IntervalDataHandler.java
│   └── NmiDataHandler.java
├── io
│   └── SqlRenderer.java
├── model
│   ├── SqlField.java
│   └── SqlRow.java
└── parser
    ├── Nem12HierarchyValidator.java
    ├── ParsingContext.java
    └── RecordIndicator.java
```

## Input and Output

Sample input:

```text
src/main/resources/data/nem12_sample_data.csv
```

Generated SQL output:

```text
output/sql/<input-file-name>.sql
```

For the sample, the output file is:

```text
output/sql/nem12_sample_data.sql
```

The output directory is created automatically if it does not exist.

## Running the Sample

`Main` is intentionally a small demo runner.
Run it from your IDE by starting:

```text
com.floenergy.assessment.nem12.Main
```

After it finishes, inspect:

```text
output/sql/nem12_sample_data.sql
```

## Running Tests

```bash
mvn test
```

Current tests cover:
- interval expansion for a simple 30-minute record
- full sample-file output against a golden SQL file
- hierarchy validation for malformed record ordering

Golden expected output for the sample file:

```text
src/test/resources/expected/nem12_sample_data.sql
```

## Key Assumptions

- The implementation follows the NEM12 interval convention of using end-of-interval timestamps.
- `300` records are validated against the active `200` record context.
- `IntervalLength` is expected to produce a full-day interval count of `1440 / IntervalLength`.
  - common valid values are `5`, `15`, and `30`
  - expected interval counts are `288`, `96`, and `48`

## Why This Design

### Why this tech stack

- I used Apache Commons CSV because it is lightweight, mature, and works well for streaming record-by-record parsing.
- I preferred it over alternatives like OpenCSV because my implementation only needs reliable CSV tokenization, not richer CSV-to-object mapping features.

### What I would do with more time

- Add broader malformed-input coverage and stricter validation of record fields beyond the currently used subset.
- Extend the tool from single-file processing to directory-level batch processing by scanning a directory of input files and processing them concurrently using a bounded thread pool, while preserving the one-input-file to one-output-file mapping.

### Why these design choices

- The parser is intentionally streaming so it does not need to load the entire NEM12 file or the generated SQL into memory, since reading the full input or accumulating the full output at once could cause excessive memory usage or even out-of-memory failures on very large files.
- The parser and SQL writer are managed with `AutoCloseable` so file handles are released reliably and buffered output is flushed even if parsing fails.
- The parser fails fast on malformed input, and SQL is written to a temporary file first and only moved to the final output path after a successful parse so invalid input does not leave behind a partial final `.sql` file.
- Responsibilities are separated so the generator coordinates the workflow, record handlers own record-specific parsing, the hierarchy validator owns ordering rules, and the SQL renderer owns output formatting.
