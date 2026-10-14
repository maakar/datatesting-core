# Extending the Framework

## Suggested Project Layout

```text
my-project/
  build.gradle
  src/main/java/
    mypackage/
      MyAnalysisApp.java
      filters/
        MyCustomFilter.java
      compare/
        MyCustomMatcher.java
      enrichment/
        MyLookupService.java
      report/
        MyTextReport.java
        MyExcelReport.java
```

## Implement a `RecordFilter`

```java
public class ActiveRecordFilter implements RecordFilter {
    private final Set<String> activeKeys;

    public ActiveRecordFilter(Set<String> activeKeys) {
        this.activeKeys = activeKeys;
    }

    public boolean test(Record r) {
        return activeKeys.contains(r.get("KEY1"));
    }

    public String name() {
        return "ActiveRecordFilter";
    }
}
```

## Implement a `FieldComparator`

```java
public class DateToleranceComparator implements FieldComparator {
    public boolean matches(String src, String tgt) {
        LocalDate d1 = LocalDate.parse(src);
        LocalDate d2 = LocalDate.parse(tgt);
        return Math.abs(ChronoUnit.DAYS.between(d1, d2)) <= 2;
    }

    public String name() {
        return "DateTolerance(2 days)";
    }
}
```

## Implement an `EnrichmentService`

```java
public class ExpectedValueLookup implements EnrichmentService {
    private final Map<String, String> valueTable;

    public EnrichmentResult enrich(Record record, Map<String, Object> ctx) {
        String value = valueTable.get(record.get("KEY1"));
        return value != null
            ? new EnrichmentResult(value, "STANDARD", "Matched")
            : EnrichmentResult.notFound("No value found");
    }

    public String name() {
        return "ExpectedValue";
    }

    public List<String> additionalColumns() {
        return List.of("EXPECTED_VALUE", "VALUE_TYPE", "COMMENT");
    }
}
```

