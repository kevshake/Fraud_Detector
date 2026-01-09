# Revenue & Income Dashboard Guide

**Date:** January 2026  
**Version:** 1.0  
**Status:** ✅ **PRODUCTION READY**

---

## Overview

The Revenue & Income Dashboard provides Platform Administrators with comprehensive insights into revenue generation from PSPs/Banks and transaction volumes. This dashboard enables financial analysis, revenue tracking, and business intelligence for the AML Fraud Detector platform.

---

## Access Requirements

### User Access
- **Platform Administrators Only**: This dashboard is exclusively for platform administrators
- **PSP Users**: Cannot access this dashboard (revenue data is confidential)
- **Authentication**: Requires platform admin credentials

### Dashboard Location
- **URL**: `/d/revenue-income`
- **Navigation**: Dashboards → Revenue & Income Dashboard
- **Tags**: `revenue`, `income`, `billing`, `psp`, `platform-admin`

---

## Dashboard Features

### 1. Revenue Overview Panels

#### **Total Revenue (All Time)**
- **Purpose**: Shows cumulative revenue from all PSPs since system inception
- **Metric**: `sum(revenue_total{psp_code=~"$PSP"})`
- **Format**: Currency (USD)
- **Use Case**: Overall platform revenue tracking

#### **Revenue (Selected Period)**
- **Purpose**: Shows revenue generated during the selected time period
- **Metric**: `sum(increase(revenue_total{psp_code=~"$PSP"}[$timeRange]))`
- **Format**: Currency (USD)
- **Time Range**: Configurable (1h, 24h, 7d, 30d)
- **Use Case**: Period-specific revenue analysis

#### **Total Transactions (Selected Period)**
- **Purpose**: Shows total transaction count during selected period
- **Metric**: `sum(increase(aml_transactions_total{psp_code=~"$PSP"}[$timeRange]))`
- **Format**: Number
- **Use Case**: Transaction volume tracking

#### **Revenue per Transaction**
- **Purpose**: Calculates average revenue per transaction
- **Metric**: Revenue / Transactions
- **Format**: Currency (USD)
- **Use Case**: Revenue efficiency analysis

---

### 2. Revenue Trends

#### **Revenue Rate by PSP**
- **Type**: Time series graph
- **Purpose**: Shows revenue generation rate over time, broken down by PSP
- **Metrics**: `sum(rate(revenue_total{psp_code=~"$PSP"}[5m])) by (psp_code)`
- **Features**:
  - Multiple PSP lines for comparison
  - Real-time updates every 5 minutes
  - Legend with last, max, and mean values
- **Use Case**: Identify high-performing PSPs and revenue trends

#### **Transaction Rate by PSP**
- **Type**: Time series graph
- **Purpose**: Shows transaction processing rate over time by PSP
- **Metrics**: `sum(rate(aml_transactions_total{psp_code=~"$PSP"}[5m])) by (psp_code)`
- **Features**:
  - Multiple PSP lines for comparison
  - Real-time updates
  - Transaction volume trends
- **Use Case**: Transaction volume analysis and capacity planning

---

### 3. Revenue Distribution

#### **Revenue Distribution by PSP (Pie Chart)**
- **Type**: Pie chart
- **Purpose**: Visual representation of revenue share by PSP
- **Metrics**: `sum(increase(revenue_total{psp_code=~"$PSP"}[$timeRange])) by (psp_code)`
- **Features**:
  - Percentage and value labels
  - Color-coded by PSP
  - Interactive legend
- **Use Case**: Quick visual assessment of PSP revenue contribution

#### **PSP Revenue & Transaction Summary Table**
- **Type**: Table
- **Purpose**: Detailed breakdown of revenue and transactions by PSP
- **Columns**:
  - PSP Code
  - Revenue (USD)
  - Transactions
  - Revenue per Transaction
- **Features**:
  - Sortable columns
  - Instant calculations
  - Exportable data
- **Use Case**: Detailed financial analysis and reporting

---

### 4. Service Type Analysis

#### **Revenue by Service Type**
- **Type**: Stacked bar chart
- **Purpose**: Shows revenue breakdown by service type
- **Service Types**:
  - Transaction Processing
  - Sanctions Screening
  - Fraud Detection
  - Compliance Cases
- **Metrics**: `sum(increase(revenue_total{psp_code=~"$PSP", service_type="..."}[$timeRange])) by (psp_code)`
- **Features**:
  - Stacked visualization
  - Service type breakdown
  - PSP comparison
- **Use Case**: Identify revenue sources and service profitability

#### **Transaction Volume by PSP**
- **Type**: Bar chart
- **Purpose**: Shows transaction volume by PSP
- **Metrics**: `sum(increase(aml_transactions_total{psp_code=~"$PSP"}[$timeRange])) by (psp_code)`
- **Features**:
  - Stacked bars for comparison
  - Volume trends
- **Use Case**: Transaction volume analysis

---

### 5. Trend Analysis

#### **Daily Revenue Trend**
- **Type**: Time series graph
- **Purpose**: Shows daily revenue trends with moving averages
- **Metrics**:
  - Daily Revenue: `sum(increase(revenue_total{psp_code=~"$PSP"}[1d]))`
  - 7-Day Average: `avg_over_time(sum(increase(revenue_total{psp_code=~"$PSP"}[1d]))[7d:1d])`
  - 30-Day Average: `avg_over_time(sum(increase(revenue_total{psp_code=~"$PSP"}[1d]))[30d:1d])`
- **Features**:
  - Daily revenue line
  - Moving average lines for trend analysis
  - Long-term trend identification
- **Use Case**: Revenue forecasting and trend analysis

#### **Daily Transaction Trend**
- **Type**: Time series graph
- **Purpose**: Shows daily transaction trends with moving averages
- **Metrics**:
  - Daily Transactions: `sum(increase(aml_transactions_total{psp_code=~"$PSP"}[1d]))`
  - 7-Day Average: `avg_over_time(sum(increase(aml_transactions_total{psp_code=~"$PSP"}[1d]))[7d:1d])`
  - 30-Day Average: `avg_over_time(sum(increase(aml_transactions_total{psp_code=~"$PSP"}[1d]))[30d:1d])`
- **Features**:
  - Daily transaction line
  - Moving average lines
  - Volume trend analysis
- **Use Case**: Transaction volume forecasting

---

## PSP Filtering

### Filter Options

1. **All PSPs** (Default)
   - Shows aggregated revenue across all PSPs
   - Platform-wide financial overview
   - Useful for total revenue tracking

2. **Single PSP Selection**
   - Shows revenue for selected PSP only
   - PSP-specific financial analysis
   - Individual PSP performance

3. **Multiple PSP Selection**
   - Compare revenue across selected PSPs
   - PSP performance benchmarking
   - Multi-tenant financial analysis

### How to Use PSP Filter

1. **Locate PSP Dropdown**: Top of dashboard, labeled "PSP"
2. **Select Filter**: Click dropdown and select PSP(s)
3. **Apply Filter**: Dashboard updates automatically
4. **Clear Filter**: Select "All PSPs" to reset

---

## Time Range Selection

### Available Time Ranges

- **Last 1 hour**: Real-time revenue monitoring
- **Last 24 hours**: Daily revenue analysis
- **Last 7 days**: Weekly revenue trends
- **Last 30 days**: Monthly revenue analysis (default)

### Custom Time Ranges

- Use time picker (top right) for custom ranges
- Select start and end dates
- Useful for specific reporting periods

---

## Use Cases

### 1. Financial Reporting
- **Monthly Revenue Reports**: Use 30-day time range
- **Quarterly Analysis**: Use custom 90-day range
- **Annual Reviews**: Use custom 365-day range

### 2. PSP Performance Analysis
- **Compare PSPs**: Select multiple PSPs for comparison
- **Identify Top Performers**: Use revenue distribution pie chart
- **Revenue Growth**: Analyze trends over time

### 3. Service Profitability
- **Service Type Analysis**: Review revenue by service type
- **Identify Revenue Sources**: Use stacked bar chart
- **Optimize Pricing**: Analyze revenue per transaction

### 4. Business Intelligence
- **Revenue Forecasting**: Use moving averages
- **Trend Analysis**: Identify growth patterns
- **Capacity Planning**: Correlate revenue with transaction volume

---

## Best Practices

### 1. Regular Monitoring
- **Daily**: Check daily revenue trends
- **Weekly**: Review weekly revenue summary
- **Monthly**: Generate monthly revenue reports

### 2. PSP Analysis
- **Compare PSPs**: Regularly compare PSP performance
- **Identify Trends**: Monitor PSP revenue trends
- **Optimize Relationships**: Focus on high-performing PSPs

### 3. Service Optimization
- **Service Type Analysis**: Review revenue by service type
- **Pricing Optimization**: Analyze revenue per transaction
- **Service Mix**: Optimize service offerings

### 4. Reporting
- **Export Data**: Export tables for external analysis
- **Snapshot Dashboards**: Save dashboard snapshots
- **Document Findings**: Document revenue insights

---

## Metrics Reference

### Revenue Metrics

```promql
# Total revenue
revenue_total{psp_code="PSP_M-PESA"}

# Revenue rate
rate(revenue_total{psp_code=~"$PSP"}[5m])

# Revenue increase over period
increase(revenue_total{psp_code=~"$PSP"}[$timeRange])

# Revenue by service type
revenue_total{psp_code=~"$PSP", service_type="TRANSACTION_PROCESSING"}
```

### Transaction Metrics

```promql
# Total transactions
aml_transactions_total{psp_code=~"$PSP"}

# Transaction rate
rate(aml_transactions_total{psp_code=~"$PSP"}[5m])

# Transaction increase over period
increase(aml_transactions_total{psp_code=~"$PSP"}[$timeRange])
```

### Calculated Metrics

```promql
# Revenue per transaction
sum(increase(revenue_total{psp_code=~"$PSP"}[$timeRange])) / 
sum(increase(aml_transactions_total{psp_code=~"$PSP"}[$timeRange]))

# Daily revenue average
avg_over_time(sum(increase(revenue_total{psp_code=~"$PSP"}[1d]))[7d:1d])
```

---

## Troubleshooting

### Issue: No Revenue Data Showing

**Symptoms**:
- Dashboard shows zero revenue
- Panels display "No data"

**Solutions**:
1. **Check Time Range**: Ensure time range includes periods with revenue
2. **Verify PSP Filter**: Check PSP filter is set correctly
3. **Check Metrics Collection**: Verify revenue metrics are being collected
4. **Review Billing Service**: Ensure billing events are being logged

### Issue: Incorrect Revenue Amounts

**Symptoms**:
- Revenue amounts seem incorrect
- Discrepancies with billing system

**Solutions**:
1. **Verify Billing Rates**: Check billing rates are configured correctly
2. **Review Service Types**: Ensure service types match billing configuration
3. **Check Currency**: Verify currency conversion if applicable
4. **Review Metrics**: Check Prometheus metrics match database records

### Issue: PSP Filter Not Working

**Symptoms**:
- Filter selection doesn't update dashboard
- Shows all PSPs regardless of filter

**Solutions**:
1. **Clear Browser Cache**: Clear cache and reload dashboard
2. **Check Variable**: Verify PSP variable is configured correctly
3. **Refresh Dashboard**: Manually refresh dashboard
4. **Check Permissions**: Verify user has platform admin access

---

## Security & Compliance

### Data Privacy
- **Confidential Data**: Revenue data is confidential and platform-admin only
- **Access Control**: Strict access control enforced
- **Audit Logging**: All dashboard access is logged

### Compliance
- **Financial Reporting**: Supports financial reporting requirements
- **Audit Trails**: Complete audit trails for revenue tracking
- **Data Retention**: Follows data retention policies

---

## Integration with Billing System

### Revenue Tracking Flow

1. **API Usage Event**: API usage tracked via `ApiUsageTrackingService`
2. **Cost Calculation**: Cost calculated via `BillingService`
3. **Revenue Recording**: Revenue recorded via `PrometheusMetricsService.recordRevenue()`
4. **Metrics Collection**: Prometheus collects revenue metrics
5. **Dashboard Display**: Grafana displays revenue in dashboard

### Service Types Tracked

- **TRANSACTION_PROCESSING**: Revenue from transaction processing
- **SANCTIONS_SCREENING**: Revenue from sanctions screening
- **FRAUD_DETECTION**: Revenue from fraud detection services
- **COMPLIANCE_CASE**: Revenue from compliance case management

---

## Summary

The Revenue & Income Dashboard provides Platform Administrators with comprehensive financial insights:

- ✅ **Total Revenue Tracking**: All-time and period-specific revenue
- ✅ **PSP Analysis**: Revenue breakdown by PSP
- ✅ **Service Type Analysis**: Revenue by service type
- ✅ **Trend Analysis**: Daily trends with moving averages
- ✅ **Transaction Correlation**: Revenue vs. transaction volume
- ✅ **PSP Filtering**: Filter by specific PSPs or view all
- ✅ **Time Range Selection**: Flexible time range analysis
- ✅ **Export Capabilities**: Export data for reporting

**Key Benefits**:
1. **Financial Visibility**: Complete revenue visibility across all PSPs
2. **Performance Analysis**: Identify top-performing PSPs
3. **Business Intelligence**: Data-driven business decisions
4. **Forecasting**: Revenue trend analysis and forecasting
5. **Optimization**: Optimize pricing and service offerings

For additional support or questions, contact the Platform Operations Team.