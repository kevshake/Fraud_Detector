# Database Query Optimization Guide

## Overview

This document outlines the database query optimization strategies, logging configuration, and performance monitoring approaches implemented in the AML Fraud Detector application.

---

## Query Logging Configuration

### Current Configuration

**File:** `src/main/resources/application.properties`

```properties
# Database Query Logging (for performance optimization)
# Enable to log slow queries (> 1 second)
spring.jpa.properties.hibernate.show_sql=${HIBERNATE_SHOW_SQL:false}
spring.jpa.properties.hibernate.format_sql=${HIBERNATE_FORMAT_SQL:true}
spring.jpa.properties.hibernate.use_sql_comments=${HIBERNATE_USE_SQL_COMMENTS:true}
# Log slow queries (requires pg_stat_statements extension)
logging.level.org.hibernate.SQL=${HIBERNATE_SQL_LOG_LEVEL:WARN}
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=${HIBERNATE_BINDER_LOG_LEVEL:WARN}
# Enable query statistics (for performance analysis)
spring.jpa.properties.hibernate.generate_statistics=${HIBERNATE_STATISTICS:false}
# Slow query threshold (milliseconds)
spring.jpa.properties.hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS=${SLOW_QUERY_THRESHOLD:1000}
```

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `HIBERNATE_SHOW_SQL` | `false` | Show all SQL queries (use only in development) |
| `HIBERNATE_FORMAT_SQL` | `true` | Format SQL queries for readability |
| `HIBERNATE_USE_SQL_COMMENTS` | `true` | Add comments to SQL queries |
| `HIBERNATE_SQL_LOG_LEVEL` | `WARN` | Log level for SQL queries |
| `HIBERNATE_STATISTICS` | `false` | Enable Hibernate statistics |
| `SLOW_QUERY_THRESHOLD` | `1000` | Threshold in milliseconds for slow queries |

---

## Enabling Query Logging

### Development Environment

```bash
export HIBERNATE_SHOW_SQL=true
export HIBERNATE_SQL_LOG_LEVEL=DEBUG
export HIBERNATE_STATISTICS=true
export SLOW_QUERY_THRESHOLD=500
```

### Production Environment

```bash
export HIBERNATE_SHOW_SQL=false
export HIBERNATE_SQL_LOG_LEVEL=WARN
export HIBERNATE_STATISTICS=false
export SLOW_QUERY_THRESHOLD=1000
```

---

## PostgreSQL Query Performance Monitoring

### 1. Enable pg_stat_statements Extension

```sql
-- Enable pg_stat_statements extension
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- View slow queries
SELECT 
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    stddev_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
```

### 2. Monitor Query Performance

```sql
-- Top 10 slowest queries
SELECT 
    substring(query, 1, 100) as query_preview,
    calls,
    round(total_exec_time::numeric, 2) as total_time_ms,
    round(mean_exec_time::numeric, 2) as avg_time_ms,
    round(max_exec_time::numeric, 2) as max_time_ms
FROM pg_stat_statements
WHERE mean_exec_time > 100  -- Queries taking more than 100ms on average
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### 3. Index Usage Analysis

```sql
-- Check index usage
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan as index_scans,
    idx_tup_read as tuples_read,
    idx_tup_fetch as tuples_fetched
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan DESC;
```

### 4. Table Statistics

```sql
-- Check table sizes and row counts
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size,
    n_live_tup as row_count
FROM pg_stat_user_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## Database Indexes

### Existing Performance Indexes

The application includes comprehensive indexes defined in `V2__Performance_Indexes.sql`:

**Audit Logs:**
- `idx_audit_timestamp` - Time-based queries
- `idx_audit_user_time` - User activity tracking
- `idx_audit_entity` - Entity change tracking
- `idx_audit_action` - Action type filtering

**Compliance Cases:**
- `idx_case_status_priority` - Dashboard queries
- `idx_case_sla_deadline` - SLA tracking
- `idx_case_assigned` - Assigned cases lookup
- `idx_case_escalated` - Escalated cases

**Transactions:**
- `idx_txn_merchant` - Merchant-based queries
- `idx_txn_timestamp` - Time-based queries
- `idx_txn_pan_hash` - PAN-based queries

**Merchants:**
- `idx_merchants_status` - Status filtering
- `idx_merchants_country` - Country-based queries
- `idx_merchants_mcc` - MCC-based queries

### Index Maintenance

```sql
-- Analyze tables to update statistics
ANALYZE;

-- Rebuild indexes if needed
REINDEX DATABASE aml_fraud_db;

-- Check index bloat
SELECT 
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY pg_relation_size(indexrelid) DESC;
```

---

## Query Optimization Best Practices

### 1. Use Indexes Effectively

✅ **Good:**
```java
// Uses index on merchant_id
List<Transaction> findByMerchantId(String merchantId);
```

❌ **Bad:**
```java
// Full table scan
@Query("SELECT t FROM Transaction t WHERE UPPER(t.merchantId) = :merchantId")
```

### 2. Avoid N+1 Query Problems

✅ **Good:**
```java
@Query("SELECT c FROM ComplianceCase c JOIN FETCH c.merchant WHERE c.id = :id")
ComplianceCase findByIdWithMerchant(Long id);
```

❌ **Bad:**
```java
// Causes N+1 queries
ComplianceCase case = caseRepository.findById(id);
Merchant merchant = merchantRepository.findById(case.getMerchantId()); // Extra query
```

### 3. Use Pagination

✅ **Good:**
```java
Page<ComplianceCase> findByStatus(String status, Pageable pageable);
```

❌ **Bad:**
```java
// Loads all records into memory
List<ComplianceCase> findAll();
```

### 4. Optimize Date Range Queries

✅ **Good:**
```java
@Query("SELECT c FROM ComplianceCase c WHERE c.createdAt BETWEEN :startDate AND :endDate")
List<ComplianceCase> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
```

### 5. Use Projections for Read-Only Queries

✅ **Good:**
```java
interface CaseSummary {
    Long getId();
    String getStatus();
    LocalDateTime getCreatedAt();
}

@Query("SELECT c.id as id, c.status as status, c.createdAt as createdAt FROM ComplianceCase c")
List<CaseSummary> findCaseSummaries();
```

---

## Performance Monitoring

### Application-Level Monitoring

**Hibernate Statistics:**
```java
// Enable in application.properties
spring.jpa.properties.hibernate.generate_statistics=true

// Access via JMX or programmatically
Statistics stats = sessionFactory.getStatistics();
long queryCount = stats.getQueryExecutionCount();
long slowQueryCount = stats.getSlowQueryCount();
```

### Database-Level Monitoring

**Query Execution Plan:**
```sql
-- Explain query execution plan
EXPLAIN ANALYZE
SELECT * FROM compliance_cases 
WHERE status = 'OPEN' 
AND priority = 'HIGH'
ORDER BY created_at DESC;
```

**Connection Pool Monitoring:**
```properties
# Monitor connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

---

## Common Performance Issues and Solutions

### Issue 1: Slow Dashboard Queries

**Symptoms:** Dashboard takes > 2 seconds to load

**Solution:**
- Ensure indexes exist on `status`, `priority`, `created_at`
- Use pagination for large datasets
- Cache frequently accessed data

### Issue 2: N+1 Query Problem

**Symptoms:** Many queries executed for single operation

**Solution:**
- Use `JOIN FETCH` in queries
- Use `@EntityGraph` annotations
- Enable batch fetching

### Issue 3: Missing Indexes

**Symptoms:** Full table scans in query plans

**Solution:**
- Analyze query execution plans
- Add indexes for frequently queried columns
- Use composite indexes for multi-column queries

### Issue 4: Large Result Sets

**Symptoms:** High memory usage, slow queries

**Solution:**
- Implement pagination
- Use projections instead of full entities
- Limit result set size

---

## Query Optimization Checklist

- [ ] Enable query logging in development
- [ ] Monitor slow queries using pg_stat_statements
- [ ] Review query execution plans
- [ ] Ensure indexes exist for frequently queried columns
- [ ] Use pagination for large datasets
- [ ] Avoid N+1 query problems
- [ ] Use projections for read-only queries
- [ ] Monitor connection pool usage
- [ ] Regularly analyze table statistics
- [ ] Review and optimize slow queries monthly

---

## Performance Targets

| Operation | Target | Current |
|-----------|--------|---------|
| Dashboard Load | < 500ms | TBD |
| Case Query | < 100ms | TBD |
| Transaction Query | < 50ms | TBD |
| Merchant Lookup | < 20ms | TBD |
| Alert Query | < 200ms | TBD |

---

## Tools and Resources

1. **PostgreSQL Tools:**
   - `pg_stat_statements` - Query statistics
   - `EXPLAIN ANALYZE` - Query execution plans
   - `pgAdmin` - Database administration

2. **Application Tools:**
   - Hibernate Statistics
   - Spring Boot Actuator
   - Application logs

3. **Monitoring:**
   - Prometheus metrics
   - Grafana dashboards
   - Custom performance dashboards

---

**Last Updated:** January 6, 2026

