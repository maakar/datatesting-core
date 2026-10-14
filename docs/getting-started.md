# Getting Started

## Prerequisites

- Java 21+
- Gradle 8.5+ (or Gradle wrapper)

## Build

```bash
./gradlew build
```

Output artifact:

- `build/libs/datatesting-core-1.0.0.jar`

## Publish

### Local file-based Maven repo

```bash
./gradlew publishMavenJavaPublicationToLocalRepository
```

### Local Maven (`~/.m2`)

```bash
./gradlew publishToMavenLocal
```

### Remote Maven repo

Uncomment/configure the remote repository in `build.gradle`, then:

```bash
./gradlew publish -PrepoUser=myuser -PrepoPassword=mypass
```

## Add to Your Project

### Gradle (Groovy)

```groovy
repositories {
    mavenCentral()
    mavenLocal()
    // Or file-based repo:
    // maven { url '/path/to/datatesting-core/build/repo' }
}

dependencies {
    implementation 'org.qamethod.datatesting:datatesting-core:1.0.0'
    implementation 'org.eclipse.collections:eclipse-collections:13.0.0'
}
```

### Gradle (Kotlin)

```kotlin
repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.qamethod.datatesting:datatesting-core:1.0.0")
    implementation("org.eclipse.collections:eclipse-collections:13.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>org.qamethod.datatesting</groupId>
    <artifactId>datatesting-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.eclipse.collections</groupId>
    <artifactId>eclipse-collections</artifactId>
    <version>13.0.0</version>
</dependency>
```

## Minimal Quick Start

```java
import java.nio.file.Path;
import org.eclipse.collections.impl.factory.Lists;
import org.qamethod.datatesting.core.compare.SortMergeCompare;
import org.qamethod.datatesting.core.db.SqliteAnalysisStore;
import org.qamethod.datatesting.core.io.CsvFileDataSource;
import org.qamethod.datatesting.core.pipeline.AnalysisPipeline;
import org.qamethod.datatesting.core.pipeline.PipelineStage;

CsvFileDataSource source = new CsvFileDataSource(Path.of("source.csv"), "Source");
CsvFileDataSource target = new CsvFileDataSource(Path.of("target.csv"), "Target");
SqliteAnalysisStore store = new SqliteAnalysisStore("jdbc:sqlite:results.db");

var columns = Lists.mutable.of("ID", "NAME", "VALUE", "DATE");
var keys = Lists.mutable.of("ID");

new AnalysisPipeline("My Comparison")
    .store(store)
    .addStage(PipelineStage.LOAD, () -> {
        store.initialize(true);
        store.recreateTable("source_data", columns, "TEXT");
        store.recreateTable("target_data", columns, "TEXT");
        store.batchInsertFromDataFrame("source_data", source.dataFrame(), columns);
        store.batchInsertFromDataFrame("target_data", target.dataFrame(), columns);
    })
    .addStage(PipelineStage.COMPARE, () -> {
        SortMergeCompare.Stats stats = new SortMergeCompare(store.getDbUrl())
            .sourceTable("source_data").targetTable("target_data")
            .keyColumns(keys).allColumns(columns)
            .execute();
        System.out.println("Matches: " + stats.perfectMatches());
        System.out.println("Mismatches: " + stats.mismatches());
    })
    .execute();
```

