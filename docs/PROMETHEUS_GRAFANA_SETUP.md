# Prometheus + Grafana Setup Guide for AML/Fraud Detector

This guide provides comprehensive instructions for setting up Prometheus and Grafana to monitor and visualize all metrics from the AML/Fraud Detector application.

## Overview

The monitoring stack includes:
- **Prometheus**: Metrics collection and storage
- **Grafana**: Visualization and dashboards
- **7 Comprehensive Dashboards**: Covering all aspects of the system

## Prerequisites

- Docker and Docker Compose installed
- AML/Fraud Detector application running on port 8080
- Ports 9090 (Prometheus) and 3000 (Grafana) available

## Quick Start

### 1. Start Prometheus and Grafana

```bash
docker-compose up -d
```

This will start:
- Prometheus on http://localhost:9090
- Grafana on http://localhost:3000

### 2. Access Grafana

- URL: http://localhost:3000
- Username: `admin`
- Password: `admin`

**Note**: Change the default password on first login.

### 3. Verify Prometheus is Scraping

1. Open http://localhost:9090
2. Go to Status → Targets
3. Verify `aml-fraud-detector` target is UP

### 4. View Dashboards in Grafana

All dashboards are automatically provisioned. Navigate to:
- Dashboards → Browse → AML Fraud Detector

## Available Dashboards

### 1. Transaction Overview Dashboard
- **Location**: `01-transaction-overview.json`
- **Metrics**:
  - Total transaction rate
  - Transaction actions distribution (blocked, allowed, held, alerted)
  - Processing time percentiles (P50, P95, P99)
  - Transactions by merchant
  - Transaction queue size
  - Transactions by risk level

### 2. AML Risk Dashboard
- **Location**: `02-aml-risk-dashboard.json`
- **Metrics**:
  - AML risk assessment rate
  - Risk level distribution (high, medium, low)
  - Assessment time percentiles
  - Active AML cases
  - Risk detection types (velocity, geographic, pattern, amount)
  - Risk level trends

### 3. Fraud Detection Dashboard
- **Location**: `03-fraud-detection-dashboard.json`
- **Metrics**:
  - Fraud assessment rate
  - Fraud detection rate
  - False positive rate
  - Fraud score distribution
  - Assessment time
  - Fraud risk factors (device, IP, behavioral, velocity)
  - Fraud score by level

### 4. Compliance Dashboard
- **Location**: `04-compliance-dashboard.json`
- **Metrics**:
  - Compliance cases created/resolved/escalated
  - Open compliance cases
  - Pending SARs
  - Cases by status and priority
  - SAR filing metrics
  - Case resolution time
  - SAR filing time

### 5. System Performance Dashboard
- **Location**: `05-system-performance-dashboard.json`
- **Metrics**:
  - API request/error rates
  - API response time percentiles
  - JVM memory usage
  - Active JVM threads
  - System uptime
  - Active connections
  - Database connection pool
  - Cache performance (hits, misses, hit rate)
  - Cache operation time

### 6. Model Performance Dashboard
- **Location**: `06-model-performance-dashboard.json`
- **Metrics**:
  - Model scoring rate (success/failure)
  - Model AUC
  - Precision at 100
  - Model score distribution
  - Model scoring time
  - Model drift score
  - Model success rate
  - Performance trends

### 7. Screening Dashboard
- **Location**: `07-screening-dashboard.json`
- **Metrics**:
  - Screening operations rate
  - Match rate
  - Screening queue size
  - Screening by type
  - Screening time percentiles
  - Match distribution
  - Screening throughput

## Configuration

### Prometheus Configuration

Edit `prometheus/prometheus.yml` to customize:
- Scrape interval
- Retention period
- Alert rules
- Target endpoints

### Grafana Configuration

Dashboards are automatically provisioned from `grafana/dashboards/`.

To add custom dashboards:
1. Create JSON file in `grafana/dashboards/`
2. Restart Grafana: `docker-compose restart grafana`

### Application Configuration

Ensure your application.properties has:
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.prometheus.enabled=true
management.metrics.export.prometheus.enabled=true
```

## Metrics Collection

The application automatically collects metrics via:
- **PrometheusMetricsService**: Comprehensive metrics service
- **MetricsAspect**: Automatic timing and counting via AOP
- **Micrometer**: Spring Boot Actuator integration

### Key Metrics Collected

#### Transaction Metrics
- `aml_transactions_total` - Total transactions
- `aml_transactions_processed_total` - Processed transactions
- `aml_transactions_blocked_total` - Blocked transactions
- `aml_transactions_allowed_total` - Allowed transactions
- `aml_transactions_held_total` - Held transactions
- `aml_transactions_alerted_total` - Alerted transactions
- `aml_transactions_processing_time` - Processing time histogram

#### AML Metrics
- `aml_risk_assessments_total` - Total risk assessments
- `aml_risk_high_total` - High risk assessments
- `aml_risk_medium_total` - Medium risk assessments
- `aml_risk_low_total` - Low risk assessments
- `aml_assessment_time` - Assessment time histogram
- `aml_active_cases` - Active AML cases gauge

#### Fraud Detection Metrics
- `fraud_assessments_total` - Total fraud assessments
- `fraud_detected_total` - Fraud detections
- `fraud_false_positive_total` - False positives
- `fraud_score_distribution` - Score distribution histogram
- `fraud_assessment_time` - Assessment time histogram

#### Compliance Metrics
- `compliance_cases_created_total` - Cases created
- `compliance_cases_resolved_total` - Cases resolved
- `compliance_open_cases` - Open cases gauge
- `compliance_pending_sars` - Pending SARs gauge
- `compliance_case_resolution_time` - Resolution time histogram

#### System Metrics
- `api_requests_total` - API requests
- `api_errors_total` - API errors
- `api_response_time` - Response time histogram
- `jvm_memory_used_bytes` - JVM memory used
- `jvm_threads_active` - Active threads
- `cache_hits_total` - Cache hits
- `cache_misses_total` - Cache misses

#### Model Metrics
- `model_scoring_total` - Total scoring operations
- `model_scoring_success_total` - Successful scoring
- `model_scoring_failure_total` - Failed scoring
- `model_auc` - Model AUC gauge
- `model_precision_at_100` - Precision at 100 gauge
- `model_drift_score` - Drift score gauge

## Alerting

Prometheus alert rules are configured in `prometheus/rules/aml_alerts.yml`.

### Available Alerts

1. **HighErrorRate** - API error rate > 10/sec
2. **HighTransactionProcessingTime** - P95 > 1000ms
3. **HighFraudDetectionRate** - Detection rate > 10%
4. **HighAMLRiskRate** - High risk rate > 5%
5. **ModelPerformanceDegradation** - AUC < 0.7
6. **HighModelDrift** - Drift score > 0.2
7. **ComplianceCasesBacklog** - Open cases > 100
8. **PendingSARsAlert** - Pending SARs > 20
9. **LowCacheHitRate** - Hit rate < 70%
10. **HighJVMMemoryUsage** - Memory usage > 90%
11. **DatabaseConnectionPoolExhaustion** - Pool usage > 90%
12. **HighScreeningQueueSize** - Queue size > 500
13. **TransactionQueueBacklog** - Queue size > 1000

### Configuring Alertmanager

To enable alert notifications, uncomment and configure Alertmanager in `prometheus/prometheus.yml`.

## Troubleshooting

### Prometheus Not Scraping

1. Check if application is running: `curl http://localhost:8080/actuator/prometheus`
2. Verify Prometheus config: Check `prometheus/prometheus.yml`
3. Check Prometheus logs: `docker-compose logs prometheus`

### Grafana Not Showing Data

1. Verify Prometheus datasource: Settings → Data Sources → Prometheus
2. Check dashboard queries: Edit panel → Query inspector
3. Verify time range: Check dashboard time picker

### Metrics Not Appearing

1. Verify metrics endpoint: `curl http://localhost:8080/actuator/prometheus`
2. Check application logs for metric collection errors
3. Verify PrometheusMetricsService is initialized

## Performance Considerations

- **Scrape Interval**: Default 10s (adjustable in prometheus.yml)
- **Retention**: Default 30 days (adjustable in docker-compose.yml)
- **Dashboard Refresh**: Default 10s (adjustable per dashboard)

## Security

### Production Recommendations

1. **Change Default Passwords**:
   - Grafana admin password
   - Application credentials

2. **Enable Authentication**:
   - Configure Grafana authentication
   - Secure Prometheus endpoint

3. **Network Security**:
   - Use internal networks
   - Restrict port access
   - Enable TLS

4. **Data Retention**:
   - Configure appropriate retention periods
   - Archive historical data

## Maintenance

### Backup Dashboards

```bash
# Export dashboards
docker exec aml-grafana grafana-cli admin export-dashboard

# Or copy from volume
docker cp aml-grafana:/var/lib/grafana/dashboards ./backup/
```

### Update Prometheus Data

```bash
# View data location
docker volume inspect aml-fraud-detector_prometheus-data

# Backup data
docker run --rm -v aml-fraud-detector_prometheus-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/prometheus-backup.tar.gz -C /data .
```

## Support

For issues or questions:
1. Check application logs
2. Review Prometheus targets status
3. Verify Grafana datasource connection
4. Check dashboard query syntax

## Next Steps

1. Customize dashboards for your needs
2. Configure alert notifications
3. Set up additional data sources
4. Create custom alert rules
5. Integrate with notification systems (Slack, PagerDuty, etc.)

