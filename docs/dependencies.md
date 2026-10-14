# Dependencies

## Transitive API Dependencies

The library exposes the following dependencies as `api`:

| Group | Artifact | Version |
|---|---|---|
| `io.github.vmzakharov` | `dataframe-ec` | 1.4.0 |
| `org.eclipse.collections` | `eclipse-collections-api` | 13.0.0 |
| `org.xerial` | `sqlite-jdbc` | 3.45.1.0 |
| `com.fasterxml.jackson.core` | `jackson-databind` | 2.17.0 |
| `com.fasterxml.jackson.dataformat` | `jackson-dataformat-yaml` | 2.17.0 |
| `com.fasterxml.jackson.datatype` | `jackson-datatype-jsr310` | 2.17.0 |

## Internal Implementation Dependencies

| Group | Artifact | Version | Purpose |
|---|---|---|---|
| `org.eclipse.collections` | `eclipse-collections` | 13.0.0 | EC implementation classes |
| `org.apache.poi` | `poi` + `poi-ooxml` | 5.2.5 | Excel report generation |

## Package Map

```text
org.qamethod.datatesting.core
├── model/          Record, RecordPair, FilterStats, EnrichmentResult
├── io/             DataSource, CsvFileDataSource
├── filter/         RecordFilter, DataFrameFilterChain
├── compare/        FieldComparator, RecordClassifier, SortMergeCompare
├── enrich/         EnrichmentService
├── db/
│   ├── AnalysisStore, SqliteAnalysisStore, TableDefinition
│   └── connector/
│       ├── DatabaseConnector, ConnectionConfig, ConnectorFactory, ConnectorRegistry
│       ├── GenericJdbcConnector, DatabaseDataSource
│       ├── auth/       AuthStrategy implementations
│       ├── azure/      AzureSqlConnector
│       ├── gcp/        GcpCloudSqlConnector
│       └── aws/        AwsRdsConnector
├── pipeline/       AnalysisPipeline, PipelineStage, PipelineListener
└── report/         ReportGenerator, TextReportWriter, ExcelReportWriter
```

