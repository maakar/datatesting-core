# Full Example

A full CSV comparison pipeline with filter, compare, and report stages.

```java
import java.nio.file.Path;
import org.eclipse.collections.impl.factory.Lists;
import org.qamethod.datatesting.core.compare.FieldComparator;
import org.qamethod.datatesting.core.compare.SortMergeCompare;
import org.qamethod.datatesting.core.db.SqliteAnalysisStore;
import org.qamethod.datatesting.core.filter.DataFrameFilterChain;
import org.qamethod.datatesting.core.filter.RecordFilter;
import org.qamethod.datatesting.core.io.CsvFileDataSource;
import org.qamethod.datatesting.core.pipeline.AnalysisPipeline;
import org.qamethod.datatesting.core.pipeline.PipelineListener;
import org.qamethod.datatesting.core.pipeline.PipelineStage;
import org.qamethod.datatesting.core.report.ExcelReportWriter;
import org.qamethod.datatesting.core.report.TextReportWriter;

public class MyAnalysis {

    public static void main(String[] args) throws Exception {
        var columns = Lists.mutable.of("KEY1", "KEY2", "VALUE", "DATE");
        var keys = Lists.mutable.of("KEY1", "KEY2");

        var source = new CsvFileDataSource(Path.of("source.csv"), "Source");
        var target = new CsvFileDataSource(Path.of("target.csv"), "Target");
        var store = new SqliteAnalysisStore("jdbc:sqlite:analysis.db");

        RecordFilter thresholdFilter = new RecordFilter() {
            public boolean test(org.qamethod.datatesting.core.model.Record r) {
                return Long.parseLong(r.get("VALUE")) > 100;
            }
            public String name() { return "ThresholdFilter"; }
        };

        var filterChain = new DataFrameFilterChain().add("threshold_filter", thresholdFilter);

        new AnalysisPipeline("Dataset Comparison")
            .store(store)
            .putMetadata("analysisDate", "2026-03-17")
            .addStage(PipelineStage.LOAD, () -> {
                store.initialize(true);
                store.recreateTable("source_data", columns, "TEXT");
                store.recreateTable("target_data", columns, "TEXT");

                var srcResult = filterChain.apply(source.dataFrame(), java.util.List.copyOf(columns), "Source");
                store.batchInsertFromDataFrame("source_data", source.dataFrame(), columns);
                store.batchInsertFromDataFrame("target_data", target.dataFrame(), columns);
            })
            .addStage(PipelineStage.COMPARE, () -> {
                var stats = new SortMergeCompare(store.getDbUrl())
                    .sourceTable("source_data")
                    .targetTable("target_data")
                    .keyColumns(keys)
                    .allColumns(columns)
                    .addCustomMatcher("VALUE", new FieldComparator() {
                        public boolean matches(String s, String t) {
                            return Math.abs(Long.parseLong(s) - Long.parseLong(t)) <= 200;
                        }
                        public String name() { return "ValueTolerance"; }
                    })
                    .execute();
                System.out.println(stats);
            })
            .addReportGenerator(new TextReportWriter("report.txt", "Dataset Analysis"))
            .addReportGenerator(new ExcelReportWriter("report.xlsx", "Dataset Analysis"))
            .addListener(new PipelineListener() {
                public void onStageComplete(PipelineStage stage, long ms) {
                    System.out.println(stage.name() + " completed in " + ms + " ms");
                }
            })
            .execute();
    }
}
```

