package org.qamethod.datatesting.core.io;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvFileDataSourceTest {

    @Test
    void loads_with_explicit_schema_and_exposes_records_metadata_and_counts() throws Exception {
        Path csv = writeCsv(
                "ID,VALUE,TYPE",
                "a,10,A",
                "b,20,B"
        );

        CsvFileDataSource ds = new CsvFileDataSource(csv, "Fixture", List.of("ID", "VALUE", "TYPE"));

        assertThat(ds.columns()).containsExactly("ID", "VALUE", "TYPE");
        assertThat(ds.recordCount()).isEqualTo(2);
        assertThat(ds.metadata())
                .containsEntry("sourceName", "Fixture")
                .containsEntry("columnCount", "3")
                .containsEntry("rowCount", "2");
        assertThat(ds.stream().map(r -> r.get("ID")).toList()).containsExactly("a", "b");
    }

    @Test
    void auto_schema_loading_reads_header_and_rows() throws Exception {
        Path csv = writeCsv(
                "ID,NAME",
                "id-1,Alice",
                "id-2,Bob"
        );

        CsvFileDataSource ds = new CsvFileDataSource(csv, "Auto");

        assertThat(ds.columns()).containsExactly("ID", "NAME");
        assertThat(ds.recordCount()).isEqualTo(2);
        assertThat(ds.stream().map(r -> r.get("NAME")).toList()).containsExactly("Alice", "Bob");
    }

    @Test
    void header_only_file_results_in_zero_records() throws Exception {
        Path csv = writeCsv("ID,VALUE,TYPE");

        CsvFileDataSource ds = new CsvFileDataSource(csv, "Empty", List.of("ID", "VALUE", "TYPE"));

        assertThat(ds.recordCount()).isZero();
        assertThat(ds.stream().toList()).isEmpty();
    }

    @Test
    void malformed_csv_throws_runtime_exception() throws Exception {
        Path csv = writeCsv(
                "ID,VALUE,TYPE",
                "a,10"
        );

        assertThatThrownBy(() -> new CsvFileDataSource(csv, "Bad", List.of("ID", "VALUE", "TYPE")))
                .isInstanceOf(RuntimeException.class);
    }

    private Path writeCsv(String... lines) throws Exception {
        Path csv = Files.createTempFile("csv-ds-", ".csv");
        Files.writeString(csv, String.join("\n", lines) + "\n");
        csv.toFile().deleteOnExit();
        return csv;
    }
}


