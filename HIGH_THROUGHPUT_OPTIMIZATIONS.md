# High Throughput Optimizations

## Overview

This document describes all optimizations implemented for high-throughput transaction processing in the AML Fraud Detector system.

## Performance Improvements

### 1. Async Processing

**Implementation:**
- `AsyncFraudDetectionOrchestrator` - Async version of fraud detection pipeline
- `AsyncConfig` - Thread pool configuration for async processing
- CompletableFuture-based pipeline for non-blocking operations

**Benefits:**
- Non-blocking request handling
- Better resource utilization
- Higher concurrent request capacity
- Reduced latency under load

**Configuration:**
```properties
async.core.pool.size=50
async.max.pool.size=200
async.queue.capacity=1000
throughput.enable.async.processing=true
```

### 2. Connection Pooling (HikariCP)

**Optimizations:**
- Minimum idle connections: 20
- Maximum pool size: 100
- Connection timeout: 30 seconds
- Idle timeout: 10 minutes
- Max lifetime: 30 minutes
- Leak detection: 60 seconds

**Benefits:**
- Reduced connection overhead
- Better connection reuse
- Automatic connection management
- Leak detection and prevention

**Configuration:**
```properties
spring.datasource.hikari.minimum-idle=20
spring.datasource.hikari.maximum-pool-size=100
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000
```

### 3. Batch Processing

**Implementation:**
- `BatchTransactionIngestionService` - Batch insert transactions
- Batch size: 100 transactions per batch
- JPA batch inserts enabled

**Benefits:**
- Reduced database round trips
- Better transaction throughput
- Optimized database writes

**Configuration:**
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
throughput.batch.size=100
```

### 4. Parallel Feature Extraction

**Implementation:**
- `OptimizedFeatureExtractionService` - Parallel feature extraction
- CompletableFuture for parallel database queries
- Independent queries executed concurrently

**Benefits:**
- Reduced feature extraction time
- Parallel database queries
- Better CPU utilization

**Configuration:**
```properties
throughput.parallel.feature.extraction=true
```

### 5. Caching

**Implementation:**
- Caffeine cache for aggregate features
- Configuration caching
- Thread-local caching for MessageDigest

**Cache Types:**
- Aggregate features cache (velocity data)
- Configuration cache (thresholds)
- Thread-local MessageDigest cache

**Benefits:**
- Reduced database queries
- Faster feature extraction
- Lower latency

**Configuration:**
```properties
cache.aggregate.max.size=100000
cache.aggregate.expire.minutes=60
cache.config.max.size=1000
cache.config.expire.minutes=30
```

### 6. Optimized PAN Hashing

**Implementation:**
- Thread-local MessageDigest cache
- Pre-allocated StringBuilder (64 chars)
- Optimized hex string building

**Benefits:**
- Reduced object allocation
- Faster hashing operations
- Lower memory overhead

### 7. Circuit Breaker (Resilience4j)

**Implementation:**
- Circuit breaker for external scoring service
- Fallback mechanism for availability
- Automatic recovery

**Benefits:**
- Prevents cascading failures
- Faster failure detection
- Graceful degradation

**Configuration:**
```properties
resilience4j.circuitbreaker.configs.default.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.default.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.configs.default.sliding-window-size=10
resilience4j.circuitbreaker.configs.default.minimum-number-of-calls=5
```

### 8. Database Query Optimization

**Optimizations:**
- Indexed queries (merchant_id, pan_hash, txn_ts)
- Batch queries where possible
- Parallel independent queries
- Query result caching

**Indexes:**
- `idx_txn_merchant` on `merchant_id`
- `idx_txn_timestamp` on `txn_ts`
- `idx_txn_pan_hash` on `pan_hash`
- `idx_features_label` on `label`
- `idx_features_scored_at` on `scored_at`

### 9. Memory Optimizations

**Optimizations:**
- Pre-allocated HashMap capacity (64)
- Pre-allocated StringBuilder capacity
- Reduced object allocations
- Efficient data structures

### 10. JSON Serialization Optimization

**Optimizations:**
- Reused ObjectMapper instances
- Efficient JSON parsing
- Minimal object creation

## Throughput Metrics

### Expected Performance

**Single Transaction:**
- Synchronous: ~50-100ms
- Async: ~30-60ms (non-blocking)

**Batch Processing:**
- 100 transactions: ~500-1000ms (parallel)
- Throughput: 100-200 TPS per instance

### Scalability

**Horizontal Scaling:**
- Multiple instances behind load balancer
- Stateless design enables easy scaling
- Database connection pooling per instance

**Vertical Scaling:**
- Thread pool tuning
- Connection pool tuning
- JVM heap optimization

## Configuration Tuning Guide

### For High Throughput (1000+ TPS)

```properties
# Thread Pools
async.core.pool.size=100
async.max.pool.size=500
async.queue.capacity=5000

# Connection Pool
spring.datasource.hikari.minimum-idle=50
spring.datasource.hikari.maximum-pool-size=200

# Batch Processing
spring.jpa.properties.hibernate.jdbc.batch_size=100
throughput.batch.size=500

# Caching
cache.aggregate.max.size=500000
cache.aggregate.expire.minutes=30
```

### For Low Latency (<50ms)

```properties
# Smaller thread pools for faster response
async.core.pool.size=20
async.max.pool.size=50

# Smaller batch sizes
throughput.batch.size=10
spring.jpa.properties.hibernate.jdbc.batch_size=10

# Aggressive caching
cache.aggregate.expire.minutes=15
```

## Monitoring

### Key Metrics to Monitor

1. **Throughput**
   - Transactions per second
   - Requests per second
   - Batch processing rate

2. **Latency**
   - P50, P95, P99 latencies
   - Feature extraction time
   - Scoring service latency
   - Database query time

3. **Resource Utilization**
   - Thread pool utilization
   - Connection pool utilization
   - Cache hit rates
   - CPU and memory usage

4. **Error Rates**
   - Circuit breaker state
   - Failed requests
   - Timeout errors

### Actuator Endpoints

- `/actuator/metrics` - System metrics
- `/actuator/health` - Health checks
- `/actuator/prometheus` - Prometheus metrics (if enabled)

## Best Practices

1. **Enable Async Processing**
   - Set `throughput.enable.async.processing=true`
   - Use async endpoints for high throughput

2. **Tune Thread Pools**
   - Match thread pool size to CPU cores
   - Monitor queue capacity
   - Adjust based on load

3. **Optimize Database**
   - Ensure indexes are present
   - Monitor query performance
   - Use connection pooling

4. **Cache Aggressively**
   - Cache frequently accessed data
   - Monitor cache hit rates
   - Adjust TTL based on data freshness needs

5. **Batch Operations**
   - Use batch endpoints for bulk operations
   - Tune batch size based on memory
   - Monitor batch processing time

6. **Circuit Breaker**
   - Configure appropriate thresholds
   - Monitor circuit breaker state
   - Implement fallback strategies

## Performance Testing

### Load Testing Recommendations

1. **Baseline Test**
   - Single transaction processing
   - Measure latency and throughput

2. **Concurrent Users**
   - Test with 100, 500, 1000 concurrent users
   - Monitor resource utilization

3. **Sustained Load**
   - Run for extended periods
   - Monitor for memory leaks
   - Check connection pool exhaustion

4. **Peak Load**
   - Test burst traffic
   - Verify graceful degradation
   - Check circuit breaker behavior

## Troubleshooting

### High Latency

1. Check database connection pool
2. Monitor thread pool queue
3. Review cache hit rates
4. Check external service latency

### Low Throughput

1. Increase thread pool size
2. Increase connection pool size
3. Enable batch processing
4. Optimize database queries

### Memory Issues

1. Reduce cache sizes
2. Reduce batch sizes
3. Monitor object allocation
4. Check for memory leaks

## Summary

All optimizations are configurable via `application.properties`:
- ✅ Async processing
- ✅ Connection pooling
- ✅ Batch processing
- ✅ Parallel feature extraction
- ✅ Caching
- ✅ Circuit breaker
- ✅ Database optimization
- ✅ Memory optimization

**Expected Throughput:** 100-200 TPS per instance (can scale horizontally)
**Expected Latency:** 30-60ms (async) or 50-100ms (sync)

