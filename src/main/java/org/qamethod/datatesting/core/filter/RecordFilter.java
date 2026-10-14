package org.qamethod.datatesting.core.filter;
import org.qamethod.datatesting.core.model.Record;

public interface RecordFilter {
    boolean test(Record record);
    String name();
    default RecordFilter and(RecordFilter other) { RecordFilter self = this; return new RecordFilter() { @Override public boolean test(Record r) { return self.test(r) && other.test(r); } @Override public String name() { return self.name() + " AND " + other.name(); } }; }
    default RecordFilter or(RecordFilter other) { RecordFilter self = this; return new RecordFilter() { @Override public boolean test(Record r) { return self.test(r) || other.test(r); } @Override public String name() { return self.name() + " OR " + other.name(); } }; }
}
