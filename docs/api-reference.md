# API Reference

## Pipeline

### `AnalysisPipeline`

Main orchestrator that executes stages in order and supports metadata, listeners, and report generators.

```java
AnalysisPipeline pipeline = new AnalysisPipeline("Pipeline Name")
    .store(analysisStore)
    .putMetadata("key", value)
    .addStage(PipelineStage.LOAD, () -> { ... })
    .addStage(PipelineStage.FILTER, () -> { ... })
    .addStage(PipelineStage.COMPARE, () -> { ... })
    .addStage(PipelineStage.CLASSIFY, () -> { ... })
    .addStage(PipelineStage.ENRICH, () -> { ... })
    .addReportGenerator(reportGenerator)
    .addListener(pipelineListener);

pipeline.execute();
```

### `PipelineStage`

- `LOAD`
- `FILTER`
- `COMPARE`
- `CLASSIFY`
- `ENRICH`
- `REPORT`

### `PipelineListener`

```java
public interface PipelineListener {
    default void onStageStart(PipelineStage stage) {}
    default void onStageComplete(PipelineStage stage, long durationMs) {}
    default void onStageError(PipelineStage stage, Exception error) {}
    default void onProgress(PipelineStage stage, long recordsProcessed, String message) {}
}
```

## Data Loading

### `DataSource`

```java
public interface DataSource extends AutoCloseable {
    List<String> columns();
    Stream<Record> stream();
    long recordCount();
    Map<String, String> metadata();
}
```

### `CsvFileDataSource`

```java
CsvFileDataSource ds = new CsvFileDataSource(Path.of("data.csv"), "MyData");
CsvFileDataSource dsWithColumns = new CsvFileDataSource(
    Path.of("data.csv"), "MyData", List.of("COL1", "COL2", "COL3"));

DataFrame df = ds.dataFrame();
long rows = ds.recordCount();
```

### `DatabaseDataSource`

```java
DatabaseDataSource ds = new DatabaseDataSource(
    connector,
    "SELECT * FROM records WHERE active = 1",
    "Records");
```

## Models

- `Record`
- `RecordPair`
- `FilterStats`
- `EnrichmentResult`

## Filtering

### `RecordFilter`

```java
public interface RecordFilter {
    boolean test(Record record);
    String name();

    default RecordFilter and(RecordFilter other) { ... }
    default RecordFilter or(RecordFilter other) { ... }
}
```

### `DataFrameFilterChain`

```java
DataFrameFilterChain chain = new DataFrameFilterChain()
    .add("primary_filter", primaryFilter)
    .add("secondary_filter", secondaryFilter)
    .add("threshold_filter", thresholdFilter);
```

## Comparison

### `FieldComparator`

```java
public interface FieldComparator {
    boolean matches(String sourceValue, String targetValue);
    String name();
}
```

### `RecordClassifier`

```java
public interface RecordClassifier {
    String classify(Record record, Map<String, Object> context);
    List<String> categories();
}
```

### `SortMergeCompare`

```java
SortMergeCompare.Stats stats = new SortMergeCompare("jdbc:sqlite:results.db")
    .sourceTable("filtered_source")
    .targetTable("filtered_target")
    .keyColumns(Lists.mutable.of("KEY1", "KEY2", "KEY3"))
    .allColumns(Lists.mutable.of("KEY1", "KEY2", "KEY3", "VALUE", "DATE"))
    .mismatchTable("mismatches")
    .execute();
```

## Storage

### `AnalysisStore`

```java
public interface AnalysisStore {
    void initialize(boolean recreateTables) throws SQLException;
    void execute(String sql) throws SQLException;
    String getDbUrl();
}
```

### `SqliteAnalysisStore`

```java
SqliteAnalysisStore store = new SqliteAnalysisStore("jdbc:sqlite:analysis.db");
store.initialize(true);
```

### `TableDefinition`

```java
TableDefinition td = new TableDefinition(
    "measurements",
    List.of("ID", "VALUE"),
    List.of("TEXT", "REAL"));
```

### `SqliteDbConsolidator`

Merges multiple SQLite analysis databases into one consolidated database with automated audit logging (`consolidation_log`).

```java
SqliteDbConsolidator consolidator = new SqliteDbConsolidator();
consolidator.consolidate(
    Path.of("consolidated.db"),
    List.of(Path.of("chunk1.db"), Path.of("chunk2.db")),
    path -> "2026-03-01"); // Function to derive analysis_date dimension
```

## Enrichment

### `EnrichmentService`

```java
public interface EnrichmentService {
    EnrichmentResult enrich(Record record, Map<String, Object> context);
    String name();
    List<String> additionalColumns();
}
```

## Reports

### `ReportGenerator`

```java
public interface ReportGenerator {
    void generate(AnalysisStore store, Map<String, Object> metadata) throws IOException;
    String outputPath();
}
```

### Implementations

- `TextReportWriter`
- `ExcelReportWriter`

