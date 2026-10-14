package org.qamethod.datatesting.core.model;

/**
 * Result of an enrichment lookup against a reference system.
 * Provides a universal contract for reference data resolution.
 *
 * @param value    The looked-up reference value (e.g., expected amount, reference code)
 * @param type     The type or strategy used for the lookup (e.g., "STANDARD", "OVERRIDE", "FALLBACK")
 * @param comment  Human-readable explanation of how the value was resolved
 */
public record EnrichmentResult(String value, String type, String comment) {

    public static EnrichmentResult notFound(String reason) {
        return new EnrichmentResult("Not Found", "N/A", reason);
    }

    public boolean isFound() {
        return !"Not Found".equals(value) && !"N/A".equals(value);
    }
}

