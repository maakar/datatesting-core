# Testing Guide

This guide details the testing architecture, conventions, and execution instructions for `datatesting-core`.

## Testing Principles

1. **Deterministic Tests:** Tests must be completely deterministic and free of race conditions or external network dependencies.
2. **Fast Feedback:** Unit tests must complete within milliseconds. Integration suites should run locally in under 15 seconds.
3. **Synthetic Test Data:** Always use synthetic, non-sensitive data files located under `src/test/resources/datasets/`. Never commit proprietary or production data.

## Test Types & Naming Conventions

The build uses naming conventions to distinguish between unit and integration tests:

| Test Type | File Pattern | Description |
| :--- | :--- | :--- |
| **Unit Tests** | `*Test.java` | Fast, in-memory tests testing isolated logic (e.g. `DataFrameFilterChainTest`, `YamlConfigLoaderTest`). |
| **Integration Tests** | `*IT.java` | End-to-end component tests using real SQLite files or file IO (e.g. `SortMergeCompareIT`, `SqliteAnalysisStoreIT`, `ExcelReportWriterIT`). |

## Running Tests Locally

### Quick Start

Run the entire test suite:

```bash
./gradlew test
```

Run clean build and tests:

```bash
./gradlew clean test
```

Run full validation including Javadoc and checks:

```bash
./gradlew check
```

### Running Specific Tests

Run an entire test class:

```bash
./gradlew test --tests "org.qamethod.datatesting.core.compare.SortMergeCompareIT"
```

Run a single test method:

```bash
./gradlew test --tests "org.qamethod.datatesting.core.filter.DataFrameFilterChainTest.apply_tracks_mask_stats_and_side_outputs"
```

Run tests matching a package pattern:

```bash
./gradlew test --tests "org.qamethod.datatesting.core.db.*"
```

### Debugging & Troubleshooting

- **Stack Traces:** Append `--stacktrace` for detailed exception traces:
  ```bash
  ./gradlew test --stacktrace
  ```
- **Verbose Output:** Append `--info` for per-test output and lifecycle logs:
  ```bash
  ./gradlew test --info
  ```
- **Stale Dependencies:** Use `--refresh-dependencies` if cache looks stale:
  ```bash
  ./gradlew test --refresh-dependencies
  ```

## Test Reports

Gradle generates comprehensive HTML and XML reports after every test execution:

- **HTML Report:** `build/reports/tests/test/index.html`
- **XML Results (for CI):** `build/test-results/test/`

To view the report in a browser on Linux:

```bash
xdg-open build/reports/tests/test/index.html
```
