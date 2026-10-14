package org.qamethod.datatesting.core.compare;

public interface FieldComparator {
    boolean matches(String sourceValue, String targetValue);
    String name();
}
