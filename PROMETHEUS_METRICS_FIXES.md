# Prometheus Metrics Service - Compilation Fixes

## Issues Found

1. **Histogram import missing** - Need to import `io.micrometer.core.instrument.DistributionSummary` instead of `Histogram`
2. **Counter.increment() with Tags** - Micrometer doesn't support this. Need to create tagged counters dynamically
3. **Gauge builder syntax** - Need to use Supplier-based approach
4. **Timer.record() with Tags** - Need to create tagged timers dynamically
5. **Repository methods** - Use individual countByStatus() calls instead of countByStatusIn()

## Quick Fixes

### 1. Replace Histogram with DistributionSummary

```java
import io.micrometer.core.instrument.DistributionSummary;

// Replace:
private final Histogram fraudScoreDistribution;

// With:
private final DistributionSummary fraudScoreDistribution;
```

### 2. Fix Counter Increment with Tags

Instead of:
```java
transactionTotalCounter.increment(Tags.of("merchant_id", merchantId));
```

Use dynamic counter creation:
```java
Counter.builder("aml.transactions.total")
    .tag("merchant_id", merchantId != null ? merchantId : "unknown")
    .register(meterRegistry)
    .increment();
```

Or create a helper method:
```java
private Counter getTaggedCounter(String name, String... tags) {
    return Counter.builder(name)
        .tags(tags)
        .register(meterRegistry);
}
```

### 3. Fix Gauge Builder

Instead of:
```java
Gauge.builder("aml.transactions.queue.size")
    .register(meterRegistry, this, PrometheusMetricsService::getTransactionQueueSize);
```

Use:
```java
Gauge.builder("aml.transactions.queue.size", transactionQueueSizeValue, AtomicLong::get)
    .register(meterRegistry);
```

### 4. Fix Repository Calls

Instead of:
```java
caseRepository.countByStatusIn(List.of(CaseStatus.NEW, CaseStatus.ASSIGNED, CaseStatus.IN_PROGRESS))
```

Use:
```java
long activeCases = caseRepository.countByStatus(CaseStatus.NEW) +
                   caseRepository.countByStatus(CaseStatus.ASSIGNED) +
                   caseRepository.countByStatus(CaseStatus.IN_PROGRESS);
```

### 5. Fix SarStatus

Use `SarStatus.PENDING_REVIEW` instead of `SarStatus.PENDING`

## Recommended Approach

For production, consider using a metrics registry pattern where you:
1. Create base counters/timers without tags
2. Use dynamic tag creation only when needed
3. Cache frequently used tagged metrics

This reduces memory overhead and improves performance.

