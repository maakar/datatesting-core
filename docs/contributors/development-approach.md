# Development Approach

This guide outlines core principles, coding standards, and architectural guidelines for contributing to `datatesting-core`.

## Core Principles

- **API Stability:** Keep public APIs stable and backward-compatible. Any breaking change must be clearly justified and coordinated.
- **Focused Changes:** Favor small, reviewable pull requests with one clear objective.
- **Simplicity:** Keep code simple and explicit over complex abstractions.
- **Immutability:** Use immutable data patterns and value objects where practical.
- **Performance:** Data comparison, filtering, and database operations are performance-sensitive paths — avoid unnecessary allocations and nested loops.

## Coding Standards

- **Language Level:** Java 21+.
- **Package Conventions:** Follow existing package layout under `org.qamethod.datatesting.core`.
- **Documentation:** Public APIs must have clear Javadoc and usage examples when non-obvious.
- **Clean Boundaries:** Avoid leaking internal implementation dependencies into public contracts.
- **Logging:** Log actionable context for failures; avoid noisy log statements in high-throughput data loops.

## Architecture Guidelines

- **Responsibility Segregation:** Keep modules cleanly separated:
  - `model`: Core data structures (`Record`, `RecordPair`, `FilterStats`, `EnrichmentResult`).
  - `io`: Data sources and input converters (`DataSource`, `CsvFileDataSource`).
  - `filter`: Data filtering engines (`RecordFilter`, `DataFrameFilterChain`).
  - `compare`: Comparison logic (`FieldComparator`, `RecordClassifier`, `SortMergeCompare`).
  - `enrich`: Data enrichment services (`EnrichmentService`).
  - `db`: Database abstraction and multi-cloud connectors (`AnalysisStore`, `SqliteAnalysisStore`, `DatabaseConnector`).
  - `pipeline`: Pipeline orchestration (`AnalysisPipeline`, `PipelineStage`, `PipelineListener`).
  - `report`: Report generation (`ReportGenerator`, `TextReportWriter`, `ExcelReportWriter`).
- **Orchestration vs. Execution:** Pipeline orchestration belongs in `pipeline`; data operations belong to their respective domain modules.
- **Extensibility:** Use interfaces for user-extension points (`RecordFilter`, `FieldComparator`, `EnrichmentService`, `PipelineListener`, `ReportGenerator`).

## Code Review Checklist

Before opening or approving a pull request, verify:

- **Correctness:** Does the behavior match the intended requirements?
- **Compatibility:** Are public APIs preserved without unintended breaking changes?
- **Safety:** Are null, empty, and edge cases handled gracefully?
- **Performance:** Are there avoidable $O(n^2)$ bottlenecks, excessive object allocations, or repeated database queries?
- **Tests:** Are unit and integration tests included for all new and modified code?
- **Documentation:** Are relevant documentation files updated?

## Definition of Done

A contribution is complete when:

1. Code is peer-reviewed and approved.
2. All CI checks pass (`./gradlew check`).
3. Unit and integration tests cover new/changed behavior.
4. Documentation in `docs/` or `README.md` is updated.
