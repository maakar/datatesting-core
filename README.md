# datatesting-core

[![CI](https://github.com/maakar/datatesting-core/actions/workflows/ci.yml/badge.svg)](https://github.com/maakar/datatesting-core/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Built with DataFrame-EC](https://img.shields.io/badge/DataFrame--EC-1.4.0-brightgreen.svg)](https://github.com/vmzakharov/dataframe-ec)
[![Eclipse Collections](https://img.shields.io/badge/Eclipse_Collections-13.0.0-orange.svg)](https://github.com/eclipse/eclipse-collections)

**Universal data testing, reconciliation, and comparison framework for Java 21.**

`datatesting-core` is an embeddable, developer-friendly Java library designed for automated data quality assurance, ETL/ELT pipeline verification, cross-database migrations, and large-scale data reconciliation.

---

## The Problem: Why Data Testing is Hard

Verifying data correctness in modern architectures (data warehouses, microservices, cloud migrations, financial feeds) is traditionally difficult:

* **Heterogeneous Silos:** SQL `EXCEPT` or `MINUS` queries cannot compare data living across different systems (e.g. comparing an on-premise CSV feed against an AWS RDS database, or Azure SQL against GCP PostgreSQL) without expensive data movement.
* **Brittle Exact Matching:** Strict equality fails on realistic data nuances — floating-point rounding errors, minor timestamp formatting differences, or expected numerical tolerance bands.
* **Filter Blindspots:** When thousands of records are filtered out before comparison, standard test scripts don't track *which* rule dropped them or why, creating hidden false positives.
* **Investigation Overhead:** When a comparison identifies mismatches, engineers spend hours writing ad-hoc SQL queries to diagnose discrepancies, while business stakeholders need executive-ready Excel reports.
* **Heavyweight Tooling:** Existing frameworks are often massive, external Python suites or heavy distributed platforms that don't integrate cleanly into native Java/JVM CI/CD pipelines and JUnit test suites.

---

## The Solution: What `datatesting-core` Does

`datatesting-core` brings software engineering discipline and test automation best practices to data quality. It models data reconciliation as a deterministic, structured, staged lifecycle:

```text
[ Data Sources ] ──► ( LOAD ) ──► ( FILTER ) ──► ( COMPARE ) ──► ( CLASSIFY & ENRICH ) ──► ( REPORT )
  CSV / DB             In-memory    Multi-rule    Sort-Merge       Root-cause tagging        Excel / Text
  Connectors           DataFrame    Filtering     Tolerances       Reference lookups         Dashboards
```

1. **Ingest:** Load data from CSV files or live databases into fast in-memory DataFrames.
2. **Filter & Track:** Apply multi-stage filter chains with per-filter record counting and isolated side-output capture.
3. **Sort-Merge Compare:** Execute high-performance sort-merge comparison with column-level custom matchers and tolerance windows.
4. **Local Analysis Store:** Persist comparison results, unmatched records, and mismatch matrices to an embedded SQLite database for immediate SQL querying.
5. **Classify & Enrich:** Categorize discrepancies and enrich diffs with expected reference values.
6. **Report:** Automatically generate formatted Excel (`.xlsx`) workbooks and text reports with summary statistics and field-level breakdowns.

---

## Key Benefits

* ⚡ **In-Process & Dependency-Light:** Runs directly in your JVM application or JUnit 5 test suite without requiring Docker, external diff services, or a Spark cluster.
* 🌐 **Cross-Source Reconciliation:** Compare CSV against DB, Cloud SQL against RDS, or staging against production seamlessly.
* 🎯 **Fine-Grained Match Tolerances:** Custom matchers (`FieldComparator`) for numeric epsilon tolerances, date offsets, and case/whitespace normalization beyond binary equality.
* 🔍 **100% Filter Traceability:** Every filter records `kept` and `filtered` metrics, and routes rejected rows into side-output tables for auditing.
* 💾 **SQL-Queryable Investigation Store:** Uses an embedded SQLite analysis store so failures can be queried with standard SQL right after test execution.
* 🚀 **Chunked Processing & Consolidation:** Avoid JVM OutOfMemory errors on massive datasets by processing in small chunks and merging results with `SqliteDbConsolidator`.
* 📊 **Stakeholder-Ready Reports:** Out-of-the-box Excel report generation with color-coded discrepancy highlights, breakdown sheets, and summary statistics.
* ☁️ **Enterprise Multi-Cloud Connectors:** Pre-configured connectors for **Azure SQL** (with Azure AD / Service Principal / Managed Identity auth), **AWS RDS** (IAM auth), **GCP Cloud SQL**, and Azure Key Vault secret resolution.

---

## Built On Open-Source Foundations

`datatesting-core` is proudly built on top of two outstanding open-source libraries in the Java data ecosystem:

### 1. [DataFrame-EC](https://github.com/vmzakharov/dataframe-ec)
Created by Vladimir Zakharov, **DataFrame-EC** is an expressive, high-performance in-memory tabular data structure library for Java.
* **Role in `datatesting-core`:** Serves as our primary in-memory tabular data abstraction. It powers high-speed CSV and DB ingestion, dynamic column projections, batch database operations, and expression-based filtering.
* **Why it matters:** It provides a lightweight, fluent alternative to heavyweight dataframe engines, keeping `datatesting-core` blazingly fast with minimal overhead.

### 2. [Eclipse Collections](https://github.com/eclipse/eclipse-collections)
Maintained by the Eclipse Foundation, **Eclipse Collections** is a best-in-class, memory-efficient collections framework for Java.
* **Role in `datatesting-core`:** Powers DataFrame-EC under the hood and provides the rich, functional collection primitives (`Lists.mutable`, optimized primitive arrays, and fast iteration algorithms) used across our pipeline stages and sort-merge comparison engine.
* **Why it matters:** Exceptional memory efficiency when handling millions of records, reducing garbage collection pressure during large-scale comparisons.

> **Transitive API Access:** Both libraries are declared as `api` dependencies in `datatesting-core`. When you add `datatesting-core` to your project, you get full access to DataFrame-EC and Eclipse Collections types without needing to add extra dependencies.

---

## Handling Large Datasets: Chunking & Memory Optimization

Comparing large data volumes (millions of rows) in Java often results in `OutOfMemoryError` or severe garbage collection freezes when whole datasets are loaded into heap memory. `datatesting-core` addresses this with a **three-tier memory-efficiency architecture**:

### 1. Disk-Backed Staging (`SqliteAnalysisStore`)
Instead of accumulating unbounded rows in RAM, raw data is batch-inserted into an embedded SQLite database using transactions (default batch size of 5,000 rows). Once rows are written to disk, heap memory is released immediately.

### 2. Streaming Sort-Merge ($O(1)$ Heap Memory)
The `SortMergeCompare` engine never loads entire datasets into memory simultaneously. It opens two sorted cursor streams (`SELECT * FROM table ORDER BY keys`) and executes a two-pointer sort-merge walk:
* **Constant memory:** Memory usage stays virtually flat whether comparing 10,000 rows or 20,000,000 rows.
* **Chunked writeback:** Discrepancies (`source_only`, `target_only`, and `mismatches`) are flushed back to SQLite tables in batches of 5,000 rows.

### 3. Chunked Processing & Consolidation (`SqliteDbConsolidator`)
For extra-large data batches (e.g. multi-month historical datasets or high-volume partitioned feeds), you can split raw data into smaller chunks, process each chunk in its own separate, isolated SQLite database (e.g., `chunk_01.db`, `chunk_02.db`), and merge them into a single consolidated database using `SqliteDbConsolidator`:

```java
import java.nio.file.Path;
import java.util.List;
import org.qamethod.datatesting.core.db.SqliteDbConsolidator;

// Merge multiple chunked SQLite databases into one consolidated report database
var consolidator = new SqliteDbConsolidator();
consolidator.consolidate(
    Path.of("consolidated_analysis.db"),
    List.of(
        Path.of("analysis_chunk_01.db"),
        Path.of("analysis_chunk_02.db"),
        Path.of("analysis_chunk_03.db")
    ),
    path -> path.getFileName().toString().replace("analysis_", "").replace(".db", "") // sets analysis_date dimension
);
```

**Key Advantages of Chunked Consolidation:**
* **Zero OOM Risk:** Keeps working memory footprint small and bounded during both filtering and comparison.
* **Built-in Audit Log:** Creates a `consolidation_log` table recording source file, chunk identifier, and merge timestamp.
* **High-Throughput Merging:** Transfers records between SQLite instances using 50,000-row batch commits.

---

## Quick Start

### 1. Build and Install

```bash
# Clone the repository
git clone https://github.com/maakar/datatesting-core.git
cd datatesting-core

# Build with Gradle wrapper (Java 21+)
./gradlew build
```

### 2. Add to Your Build

**Gradle (Groovy):**
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.qamethod.datatesting:datatesting-core:1.0.0'
}
```

**Maven:**
```xml
<dependency>
    <groupId>org.qamethod.datatesting</groupId>
    <artifactId>datatesting-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3. Complete Pipeline Example

```java
import java.nio.file.Path;
import org.eclipse.collections.impl.factory.Lists;
import org.qamethod.datatesting.core.compare.SortMergeCompare;
import org.qamethod.datatesting.core.db.SqliteAnalysisStore;
import org.qamethod.datatesting.core.io.CsvFileDataSource;
import org.qamethod.datatesting.core.pipeline.AnalysisPipeline;
import org.qamethod.datatesting.core.pipeline.PipelineStage;
import org.qamethod.datatesting.core.report.ExcelReportWriter;
import org.qamethod.datatesting.core.report.TextReportWriter;

public class DataReconciliationDemo {
    public static void main(String[] args) throws Exception {
        var columns = Lists.mutable.of("TRADE_ID", "ACCOUNT", "AMOUNT", "CURRENCY");
        var keys = Lists.mutable.of("TRADE_ID");

        // 1. Define data sources
        var source = new CsvFileDataSource(Path.of("source_feed.csv"), "SourceFeed");
        var target = new CsvFileDataSource(Path.of("target_feed.csv"), "TargetFeed");
        
        // 2. Initialize embedded SQLite analysis store
        var store = new SqliteAnalysisStore("jdbc:sqlite:reconciliation.db");

        // 3. Orchestrate comparison pipeline
        new AnalysisPipeline("Trade Feed Reconciliation")
            .store(store)
            .putMetadata("environment", "STAGING")
            .addStage(PipelineStage.LOAD, () -> {
                store.initialize(true);
                store.recreateTable("source_trades", columns, "TEXT");
                store.recreateTable("target_trades", columns, "TEXT");
                store.batchInsertFromDataFrame("source_trades", source.dataFrame(), columns);
                store.batchInsertFromDataFrame("target_trades", target.dataFrame(), columns);
            })
            .addStage(PipelineStage.COMPARE, () -> {
                SortMergeCompare.Stats stats = new SortMergeCompare(store.getDbUrl())
                    .sourceTable("source_trades")
                    .targetTable("target_trades")
                    .keyColumns(keys)
                    .allColumns(columns)
                    .mismatchTable("trade_mismatches")
                    .execute();

                System.out.println("Perfect Matches: " + stats.perfectMatches());
                System.out.println("Mismatches:      " + stats.mismatches());
                System.out.println("Source Only:     " + stats.sourceOnly());
                System.out.println("Target Only:     " + stats.targetOnly());
            })
            .addReportGenerator(new TextReportWriter("reconciliation-summary.txt", "Trade Report"))
            .addReportGenerator(new ExcelReportWriter("reconciliation-report.xlsx", "Trade Report"))
            .execute();
    }
}
```

---

## Documentation

* **Guides & References:**
  * [`docs/index.md`](docs/index.md) - Documentation hub
  * [`docs/getting-started.md`](docs/getting-started.md) - Detailed setup and build instructions
  * [`docs/api-reference.md`](docs/api-reference.md) - Core interfaces and lifecycle reference
  * [`docs/database-connectors.md`](docs/database-connectors.md) - Multi-cloud DB providers and authentication modes
  * [`docs/examples.md`](docs/examples.md) - End-to-end pipeline with custom tolerance comparison
  * [`docs/extending.md`](docs/extending.md) - Implementing custom filters, matchers, and enrichment services
  * [`docs/dependencies.md`](docs/dependencies.md) - Transitive dependency map and package architecture
* **Contributor Guides:**
  * [`docs/contributors/development-approach.md`](docs/contributors/development-approach.md) - Principles and coding standards
  * [`docs/contributors/testing-guide.md`](docs/contributors/testing-guide.md) - Test architecture and local execution guide
  * [`docs/contributors/branching-strategy.md`](docs/contributors/branching-strategy.md) - Git workflow and release process

---

## Contributing

Contributions are very welcome! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for instructions on setting up your environment, running tests, and opening pull requests.

Please also review our [Code of Conduct](CODE_OF_CONDUCT.md) and [Security Policy](SECURITY.md).

---

## License

`datatesting-core` is open-source software licensed under the **[Apache License, Version 2.0](LICENSE)**.
