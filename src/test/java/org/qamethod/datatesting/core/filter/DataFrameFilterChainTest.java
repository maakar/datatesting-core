package org.qamethod.datatesting.core.filter;

import org.junit.jupiter.api.Test;
import org.qamethod.datatesting.core.io.CsvFileDataSource;
import org.qamethod.datatesting.core.model.FilterStats;
import org.qamethod.datatesting.core.model.Record;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataFrameFilterChainTest {

    @Test
    void apply_tracks_mask_stats_and_side_outputs() throws Exception {
        Path csvPath = Files.createTempFile("filter-chain-", ".csv");
        Files.writeString(csvPath, "ID,VALUE,TYPE\n"
                + "a,10,A\n"
                + "b,50,B\n"
                + "c,150,C\n"
                + "d,250,D\n");
        csvPath.toFile().deleteOnExit();
        CsvFileDataSource source = new CsvFileDataSource(csvPath, "TestSource", List.of("ID", "VALUE", "TYPE"));

        RecordFilter keepValueAtLeast100 = new NamedFilter("ge100") {
            @Override
            public boolean test(Record r) {
                return Long.parseLong(r.get("VALUE")) >= 100;
            }
        };
        RecordFilter keepValueBelow200 = new NamedFilter("lt200") {
            @Override
            public boolean test(Record r) {
                return Long.parseLong(r.get("VALUE")) < 200;
            }
        };

        DataFrameFilterChain.Result result = new DataFrameFilterChain()
                .add("ge100", keepValueAtLeast100)
                .add("lt200", keepValueBelow200)
                .apply(source.dataFrame(), source.columns(), "TestSource");

        FilterStats stats = result.stats();
        assertThat(stats.totalRecords()).isEqualTo(4);
        assertThat(stats.keptRecords()).isEqualTo(1);
        assertThat(stats.totalFiltered()).isEqualTo(3);
        assertThat(stats.filteredByCategory()).containsEntry("ge100", 2L).containsEntry("lt200", 1L);

        assertThat(result.keptMask()).containsExactly(false, false, true, false);

        assertThat(result.sideOutput("ge100")).hasSize(2);
        assertThat(result.sideOutput("lt200")).hasSize(1);
        assertThat(result.sideOutput("lt200").get(0).get("ID")).isEqualTo("d");
        assertThat(result.sideOutput("missing")).isEmpty();
    }

    @Test
    void apply_handles_empty_input() throws Exception {
        CsvFileDataSource source = dataSourceFromLines(
                "ID,VALUE,TYPE"
        );

        RecordFilter alwaysTrue = new NamedFilter("all") {
            @Override
            public boolean test(Record r) {
                return true;
            }
        };

        DataFrameFilterChain.Result result = new DataFrameFilterChain()
                .add("all", alwaysTrue)
                .apply(source.dataFrame(), source.columns(), "EmptySource");

        assertThat(result.stats().totalRecords()).isZero();
        assertThat(result.stats().keptRecords()).isZero();
        assertThat(result.keptMask()).isEmpty();
        assertThat(result.sideOutput("all")).isEmpty();
    }

    @Test
    void apply_all_pass_and_all_fail_cases() throws Exception {
        CsvFileDataSource source = dataSourceFromLines(
                "ID,VALUE,TYPE",
                "a,10,A",
                "b,20,B"
        );

        RecordFilter alwaysTrue = new NamedFilter("pass") {
            @Override
            public boolean test(Record r) {
                return true;
            }
        };
        RecordFilter alwaysFalse = new NamedFilter("fail") {
            @Override
            public boolean test(Record r) {
                return false;
            }
        };

        DataFrameFilterChain.Result passResult = new DataFrameFilterChain()
                .add("pass", alwaysTrue)
                .apply(source.dataFrame(), source.columns(), "AllPass");
        assertThat(passResult.stats().keptRecords()).isEqualTo(2);
        assertThat(passResult.stats().totalFiltered()).isZero();

        DataFrameFilterChain.Result failResult = new DataFrameFilterChain()
                .add("fail", alwaysFalse)
                .apply(source.dataFrame(), source.columns(), "AllFail");
        assertThat(failResult.stats().keptRecords()).isZero();
        assertThat(failResult.stats().totalFiltered()).isEqualTo(2);
        assertThat(failResult.sideOutput("fail")).hasSize(2);
    }

    @Test
    void apply_propagates_malformed_value_errors_from_filter_logic() throws Exception {
        CsvFileDataSource source = dataSourceFromLines(
                "ID,VALUE,TYPE",
                "a,not_a_number,A"
        );

        RecordFilter numericFilter = new NamedFilter("numeric") {
            @Override
            public boolean test(Record r) {
                return Long.parseLong(r.get("VALUE")) > 0;
            }
        };

        assertThatThrownBy(() -> new DataFrameFilterChain()
                .add("numeric", numericFilter)
                .apply(source.dataFrame(), source.columns(), "Malformed"))
                .isInstanceOf(NumberFormatException.class);
    }

    private CsvFileDataSource dataSourceFromLines(String... lines) throws Exception {
        Path csvPath = Files.createTempFile("filter-chain-fixture-", ".csv");
        Files.writeString(csvPath, String.join("\n", lines) + "\n");
        csvPath.toFile().deleteOnExit();
        return new CsvFileDataSource(csvPath, "Fixture", List.of("ID", "VALUE", "TYPE"));
    }

    private abstract static class NamedFilter implements RecordFilter {
        private final String name;

        private NamedFilter(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }
}

