package org.qamethod.datatesting.core.compare;
import org.qamethod.datatesting.core.model.Record;
import java.util.List;
import java.util.Map;

public interface RecordClassifier {
    String classify(Record record, Map<String, Object> context);
    List<String> categories();
}
