package org.qamethod.datatesting.core.enrich;
import org.qamethod.datatesting.core.model.EnrichmentResult;
import org.qamethod.datatesting.core.model.Record;
import java.util.List;
import java.util.Map;

public interface EnrichmentService {
    EnrichmentResult enrich(Record record, Map<String, Object> context);
    String name();
    List<String> additionalColumns();
}
