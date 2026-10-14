package org.qamethod.datatesting.core.compare;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NumericComparisonUtilsTest {

    @Test
    void valuesMatch_returns_true_within_tolerance() {
        assertThat(NumericComparisonUtils.valuesMatch("10.001", "10.004", 0.005)).isTrue();
        assertThat(NumericComparisonUtils.valuesMatch("10.0", "10.0", 0.0)).isTrue();
        assertThat(NumericComparisonUtils.valuesMatch("-5.5", "-5.2", 0.5)).isTrue();
    }

    @Test
    void valuesMatch_returns_false_outside_tolerance_or_on_invalid_input() {
        assertThat(NumericComparisonUtils.valuesMatch("10.001", "10.010", 0.005)).isFalse();
        assertThat(NumericComparisonUtils.valuesMatch(null, "10.0", 0.1)).isFalse();
        assertThat(NumericComparisonUtils.valuesMatch("10.0", null, 0.1)).isFalse();
        assertThat(NumericComparisonUtils.valuesMatch("abc", "10.0", 0.1)).isFalse();
    }

    @Test
    void truncateForComparison_handles_valid_and_edge_cases() {
        assertThat(NumericComparisonUtils.truncateForComparison("2026-03-01 12:30:45", 3)).isEqualTo("2026-03-01 12:30");
        assertThat(NumericComparisonUtils.truncateForComparison("hello", 0)).isEqualTo("hello");
        assertThat(NumericComparisonUtils.truncateForComparison("hello", -1)).isEqualTo("hello");
        assertThat(NumericComparisonUtils.truncateForComparison("hello", 10)).isEqualTo("hello");
        assertThat(NumericComparisonUtils.truncateForComparison(null, 2)).isNull();
    }
}
