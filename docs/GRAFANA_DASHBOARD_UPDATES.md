# Grafana Dashboard and Datasource Updates

**Date:** January 2026  
**Status:** ✅ **COMPLETE**

## Overview

This document summarizes the comprehensive updates made to Grafana dashboards and Prometheus datasources based on:
- Application structure changes and new features
- Industry best practices for Prometheus and Grafana
- Spring Boot Actuator metric naming conventions
- Ultra-high throughput optimizations (30K+ concurrent requests)
- New infrastructure components (Aerospike, Circuit Breakers, Thread Pools)

---

## 1. Prometheus Datasource Configuration Updates

### Enhanced Configuration (`grafana/datasources/prometheus.yml`)

**Improvements:**
- ✅ Added query timeout (60s) for long-running queries
- ✅ Added exemplar trace support for distributed tracing integration
- ✅ Enhanced HTTP method configuration (POST for better performance)
- ✅ Time interval optimized to 10s

**Key Features:**
- Query timeout prevents dashboard hangs
- Exemplar support enables trace correlation
- POST method reduces URL length limits

---

## 2. Prometheus Configuration Updates

### Optimized Scraping (`prometheus/prometheus.yml`)

**Changes:**
- ✅ Scrape interval: 15s (optimized for high-throughput systems)
- ✅ Scrape timeout: 10s (prevents hanging scrapes)
- ✅ Metric relabeling to filter only relevant metrics (reduces storage)
- ✅ Enhanced external labels for better organization
- ✅ Service discovery ready configuration

**Metric Filtering:**
Only relevant metrics are stored:
- `aml_*` - AML-specific metrics
- `fraud_*` - Fraud detection metrics
- `compliance_*` - Compliance metrics
- `screening_*` - Screening metrics
- `model_*` - ML model metrics
- `api_*` - API metrics
- `cache_*` - Cache metrics
- `jvm_*` - JVM metrics
- `http_server_*` - HTTP server metrics
- `hikaricp_*` - HikariCP connection pool metrics
- `process_*` - Process metrics
- `system_*` - System metrics
- `database_*` - Database metrics
- `circuitbreaker_*` - Circuit breaker metrics
- `thread_pool_*` - Thread pool metrics

---

## 3. New Dashboards Created

### 3.1 Infrastructure & Resources Dashboard
**File:** `08-infrastructure-resources-dashboard.json`

**Metrics Monitored:**
- JVM Memory Utilization (Heap & Non-Heap)
- Active JVM Threads
- HikariCP Connection Pool Status
- Connection Pool Utilization
- Connection Pool Health Metrics
- JVM Thread States (Live, Daemon, Peak, Blocked)
- System Uptime
- GC Pause Rate
- GC Pause Duration (P50, P95, P99)

**Key Features:**
- Real-time gauges for critical metrics
- Time-series graphs for trend analysis
- Proper threshold configurations
- Uses Spring Boot Actuator metric names

### 3.2 Thread Pools & High Throughput Dashboard
**File:** `09-thread-pools-throughput-dashboard.json`

**Metrics Monitored:**
- Ultra Throughput Thread Pool Utilization
- Total Queued Tasks (All Ultra Pools)
- Tasks Completed/sec (All Ultra Pools)
- Rejected Tasks/sec (Backpressure Indicator)
- Individual Pool Metrics:
  - Ultra Transaction Executor
  - Feature Extraction Executor
  - Scoring Executor
- Queue Sizes per Executor
- Task Completion Rates
- Aggregate Throughput Metrics

**Key Features:**
- Monitors 30K+ concurrent request capacity
- Tracks thread pool saturation
- Backpressure indicators
- Throughput correlation with transaction processing

### 3.3 Circuit Breaker & Resilience Dashboard
**File:** `10-circuit-breaker-resilience-dashboard.json`

**Metrics Monitored:**
- Circuit Breaker States (CLOSED, OPEN, HALF_OPEN, DISABLED, FORCED_OPEN)
- Slow Call Rate
- Failure Rate
- Calls Not Permitted (when circuit is open)
- Successful vs Failed Calls
- Buffered Calls
- Slow Calls
- Circuit Breaker State Summary

**Key Features:**
- Visual state indicators with color coding
- Failure rate tracking
- Slow call detection
- Resilience pattern monitoring

---

## 4. Updated Existing Dashboards

### 4.1 System Performance Dashboard
**File:** `05-system-performance-dashboard.json` (Updated to v2)

**Improvements:**
- ✅ Updated to use Spring Boot Actuator metrics (`http.server.requests`)
- ✅ Combined Spring Boot HTTP metrics with custom API metrics
- ✅ Updated HikariCP metric names (`hikaricp_connections_*`)
- ✅ Updated JVM metric names (`jvm_threads_live_threads`, `jvm_memory_*`)
- ✅ Enhanced cache metrics with proper bucket queries
- ✅ Modern Grafana schema v38
- ✅ Better visualization with thresholds and color coding

**Metrics Updated:**
- HTTP Request Rate: `http_server_requests_seconds_count`
- HTTP Error Rate: `http_server_requests_seconds_count{status=~"5.."}`
- Response Time: `http_server_requests_seconds_bucket` (histogram)
- JVM Memory: `jvm_memory_used_bytes{area="heap"}` and `area="nonheap"`
- HikariCP: `hikaricp_connections_active`, `hikaricp_connections_idle`, `hikaricp_connections_max`, `hikaricp_connections_pending`

---

## 5. Best Practices Implemented

### 5.1 Dashboard Design
- ✅ **Schema Version 38**: Latest Grafana dashboard schema
- ✅ **Proper Panel Sizing**: Optimal grid positions (24 columns)
- ✅ **Color Coding**: Threshold-based color schemes
- ✅ **Legend Configuration**: Table format with calculations (mean, max, lastNotNull)
- ✅ **Tooltip Configuration**: Multi-series tooltip for better analysis
- ✅ **Refresh Intervals**: Multiple options (5s, 10s, 30s, 1m, 5m, 15m, 30m, 1h, 2h, 1d)

### 5.2 PromQL Query Optimization
- ✅ **Rate Functions**: Proper use of `rate()` with 5-minute windows
- ✅ **Histogram Quantiles**: Correct P50, P95, P99 calculations
- ✅ **Label Filtering**: Efficient label-based filtering
- ✅ **Aggregations**: Proper use of `sum()`, `by()` clauses
- ✅ **Time Windows**: Appropriate time ranges for different metrics

### 5.3 Metric Naming Conventions
- ✅ **Spring Boot Actuator**: Uses standard Actuator metric names
- ✅ **Custom Metrics**: Follows Micrometer naming conventions
- ✅ **Label Consistency**: Consistent label usage across dashboards
- ✅ **Application Tag**: All metrics tagged with `application="aml-fraud-detector"`

### 5.4 Performance Optimization
- ✅ **Metric Filtering**: Only relevant metrics stored in Prometheus
- ✅ **Scrape Interval**: Optimized to 15s (balances freshness vs load)
- ✅ **Query Timeouts**: Prevents long-running queries from hanging
- ✅ **Efficient Aggregations**: Reduced cardinality with proper label usage

---

## 6. Metrics Coverage

### 6.1 Application Metrics
- ✅ Transaction Processing (total, processed, blocked, allowed, held, alerted)
- ✅ AML Risk Assessments (high, medium, low)
- ✅ Fraud Detection (assessments, detections, false positives)
- ✅ Compliance Cases (created, resolved, escalated, open)
- ✅ SAR Filing (created, filed, approved, rejected)
- ✅ Screening Operations (total, matches, by type)
- ✅ Model Performance (scoring, AUC, precision, drift)

### 6.2 Infrastructure Metrics
- ✅ HTTP Server Metrics (requests, errors, response times)
- ✅ JVM Metrics (memory, threads, GC)
- ✅ Database Connection Pool (HikariCP)
- ✅ Thread Pools (ultra-high throughput executors)
- ✅ Cache Performance (hits, misses, hit rate)
- ✅ Circuit Breakers (state, failure rate, slow calls)

### 6.3 System Metrics
- ✅ System Uptime
- ✅ Active Connections
- ✅ Thread Pool Utilization
- ✅ Connection Pool Utilization
- ✅ Cache Hit Rates
- ✅ GC Performance

---

## 7. Dashboard Organization

### Dashboard Tags
- `infrastructure` - Infrastructure and resource dashboards
- `performance` - Performance-related dashboards
- `thread-pools` - Thread pool monitoring
- `circuit-breaker` - Resilience patterns
- `resilience` - System reliability
- `system` - System-level metrics
- `api` - API performance
- `jvm` - JVM metrics
- `hikaricp` - Database connection pool

### Dashboard List
1. **Transaction Overview** - Transaction processing metrics
2. **AML Risk Dashboard** - AML risk assessment metrics
3. **Fraud Detection Dashboard** - Fraud detection metrics
4. **Compliance Dashboard** - Compliance case management
5. **System Performance Dashboard** - System and API performance ⭐ **UPDATED**
6. **Model Performance Dashboard** - ML model metrics
7. **Screening Dashboard** - Screening operations
8. **Infrastructure & Resources** ⭐ **NEW**
9. **Thread Pools & High Throughput** ⭐ **NEW**
10. **Circuit Breaker & Resilience** ⭐ **NEW**

---

## 8. Future Enhancements

### Recommended Additions
1. **Aerospike Metrics Dashboard** (when Aerospike is enabled)
   - Connection status
   - Query performance
   - Namespace metrics
   - Cluster health

2. **HTTP/2 Health Dashboard**
   - HTTP/2 connection health
   - Failover events
   - Network stability metrics

3. **Geographic Risk Heatmap Dashboard**
   - Country-level risk visualization
   - Risk distribution maps
   - Geographic anomaly detection

4. **Alerting Dashboard**
   - Active alerts
   - Alert history
   - Alert resolution times

5. **Business Metrics Dashboard**
   - Transaction volume by merchant
   - Risk distribution trends
   - Compliance case resolution times
   - SAR filing trends

---

## 9. Usage Instructions

### Accessing Dashboards
1. Navigate to Grafana: http://localhost:3000
2. Login with: `admin` / `admin` (change on first login)
3. Go to: **Dashboards → Browse → AML Fraud Detector**
4. Select any dashboard from the list

### Customizing Dashboards
- Dashboards are editable in Grafana UI
- Changes can be exported and saved back to JSON files
- Variables can be added for filtering (instance, merchant, etc.)

### Setting Up Alerts
1. Use Prometheus alert rules (`prometheus/rules/aml_alerts.yml`)
2. Configure Alertmanager (if needed)
3. Create alert panels in dashboards
4. Set up notification channels (email, Slack, PagerDuty)

---

## 10. Validation Checklist

- ✅ All dashboards use correct metric names
- ✅ PromQL queries are optimized and efficient
- ✅ Thresholds are configured appropriately
- ✅ Color coding follows best practices
- ✅ Dashboards refresh automatically
- ✅ Legend and tooltip configurations are optimal
- ✅ Schema version is up-to-date (v38)
- ✅ Panel layouts are well-organized
- ✅ Metrics are properly filtered in Prometheus
- ✅ Datasource configuration is optimized

---

## 11. Troubleshooting

### Dashboard Not Loading Metrics
1. Check Prometheus is scraping: http://localhost:9090/targets
2. Verify metric names: http://localhost:9090/graph
3. Check label filters match actual metric labels
4. Verify application is exposing metrics at `/actuator/prometheus`

### Metrics Missing
1. Ensure Spring Boot Actuator is enabled
2. Check `management.endpoints.web.exposure.include=prometheus`
3. Verify `PrometheusMetricsService` is collecting metrics
4. Check application logs for metric collection errors

### High Cardinality Issues
1. Review metric relabeling configuration
2. Avoid high-cardinality labels (timestamps, IDs)
3. Use aggregation functions to reduce series
4. Monitor Prometheus storage growth

---

## Summary

**Total Dashboards:** 10 (3 new, 1 updated)  
**Metrics Tracked:** 100+  
**Prometheus Configuration:** ✅ Optimized  
**Grafana Datasource:** ✅ Enhanced  
**Best Practices:** ✅ Implemented  
**Production Ready:** ✅ Yes

All dashboards follow industry best practices and are ready for production use. The monitoring setup provides comprehensive visibility into the AML Fraud Detector application performance, infrastructure health, and business metrics.
