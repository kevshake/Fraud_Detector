# Grafana Dashboard Access Guide for PSPs/Banks

**Date:** January 2026  
**Version:** 1.0  
**Status:** ✅ **PRODUCTION READY**

---

## Table of Contents

1. [Overview](#overview)
2. [Access Control & User Roles](#access-control--user-roles)
3. [Getting Started](#getting-started)
4. [Dashboard Access by User Type](#dashboard-access-by-user-type)
5. [PSP Filtering Guide](#psp-filtering-guide)
6. [Dashboard Descriptions](#dashboard-descriptions)
7. [Step-by-Step Access Instructions](#step-by-step-access-instructions)
8. [Best Practices](#best-practices)
9. [Troubleshooting](#troubleshooting)
10. [Security & Compliance](#security--compliance)

---

## 1. Overview

The AML Fraud Detector Grafana dashboards provide comprehensive monitoring and analytics for Payment Service Providers (PSPs) and Banks. The system implements **complete multi-tenant isolation**, ensuring each PSP can only access their own transaction metrics, risk assessments, and performance data.

### Key Features

- **Multi-Tenant Isolation**: Each PSP sees only their own data
- **Real-Time Monitoring**: Live metrics updated every 10-15 seconds
- **10 Comprehensive Dashboards**: Covering transactions, fraud, AML, compliance, and infrastructure
- **PSP Filtering**: Easy filtering by PSP/Bank for platform administrators
- **Security Compliant**: Complete data segregation for regulatory compliance

---

## 2. Access Control & User Roles

### User Types

#### **PSP Users** (Tenant Users)
- **Access Level**: PSP-specific dashboards only
- **Data Visibility**: Only their PSP's transaction and performance metrics
- **Permissions**: Read-only dashboard access
- **Use Case**: PSP operations teams, compliance officers, risk managers

#### **Platform Administrators** (System Admins)
- **Access Level**: All dashboards with full filtering capabilities
- **Data Visibility**: All PSPs or filtered by specific PSP
- **Permissions**: Full dashboard access, can view all PSPs or filter
- **Use Case**: Platform operations, system monitoring, multi-tenant management

#### **Compliance Officers**
- **Access Level**: PSP-specific compliance dashboards
- **Data Visibility**: PSP-specific compliance cases, SARs, regulatory metrics
- **Permissions**: Read-only access to compliance-related dashboards
- **Use Case**: AML compliance monitoring, regulatory reporting

---

## 3. Getting Started

### Prerequisites

1. **Valid User Account**: PSP user account or platform admin account
2. **Grafana Access**: Grafana URL and credentials
3. **Network Access**: Access to Grafana instance (typically internal network)
4. **Browser**: Modern web browser (Chrome, Firefox, Edge, Safari)

### Initial Login

1. **Navigate to Grafana URL**
   ```
   https://grafana.your-domain.com
   ```
   Or for local development:
   ```
   http://localhost:3000
   ```

2. **Login Credentials**
   - **PSP Users**: Use your PSP-specific credentials
   - **Platform Admins**: Use platform admin credentials
   - **Default Admin**: `admin` / `admin` (change on first login)

3. **First-Time Setup**
   - Change default password if prompted
   - Select your preferred timezone
   - Configure notification preferences

---

## 4. Dashboard Access by User Type

### For PSP Users

**Available Dashboards:**
- ✅ Transaction Overview Dashboard
- ✅ AML Risk Dashboard
- ✅ Fraud Detection Dashboard
- ✅ Compliance Dashboard
- ✅ Model Performance Dashboard
- ✅ Screening Dashboard
- ✅ System Performance Dashboard (PSP-filtered API metrics)
- ❌ Infrastructure Resources Dashboard (Platform Admin only)
- ❌ Thread Pools Throughput Dashboard (Platform Admin only)
- ❌ Circuit Breaker Resilience Dashboard (Platform Admin only)

**Data Visibility:**
- **Automatically Filtered**: All dashboards show only your PSP's data
- **No Cross-PSP Access**: Cannot view other PSPs' metrics
- **No PSP Dropdown**: PSP filter dropdown is hidden - you can only see your PSP
- **Real-Time Updates**: Metrics update automatically every 10-15 seconds

**Important**: PSP users **CANNOT** select or view other PSPs' metrics. The PSP dropdown is hidden and queries are automatically filtered to your PSP only.

### For Platform Administrators

**Available Dashboards:**
- ✅ All 10 dashboards with full access
- ✅ PSP filtering dropdown on all dashboards
- ✅ Global view (all PSPs) or filtered view (specific PSP)
- ✅ Infrastructure monitoring dashboards

**Data Visibility:**
- **Global View**: View all PSPs' aggregated metrics
- **PSP Filtering**: Filter by specific PSP using dropdown (visible only to Platform Admins)
- **Multi-PSP Comparison**: Compare performance across PSPs
- **System Monitoring**: Full infrastructure and system metrics

**Important**: Platform Administrators have **exclusive access** to the PSP dropdown filter. PSP users cannot see or use this filter.

---

## 5. PSP Filtering Guide

### ⚠️ IMPORTANT: Role-Based Access

**PSP Users**: 
- ❌ **PSP dropdown is HIDDEN** - You cannot see or use the PSP filter
- ✅ **Automatic Filtering** - Your dashboards are automatically filtered to your PSP only
- ❌ **No Cross-PSP Access** - You cannot view other PSPs' metrics

**Platform Administrators**:
- ✅ **PSP dropdown is VISIBLE** - You can filter by PSP(s)
- ✅ **View All PSPs** - Select "All PSPs" for platform-wide view
- ✅ **Filter by PSP** - Select specific PSP(s) for filtered view

### Understanding PSP Filtering (Platform Admins Only)

Platform Admin dashboards include a **PSP dropdown filter** at the top of the dashboard:

```
┌─────────────────────────────────────────┐
│ PSP: [All PSPs ▼]                       │
│                                         │
│ ☑ All PSPs                              │
│ ☐ PSP_M-PESA                            │
│ ☐ PSP_PAYPAL                            │
│ ☐ PSP_STRIPE                            │
└─────────────────────────────────────────┘
```

### Filter Options

#### **"All PSPs"** (Default for Platform Admins)
- Shows aggregated metrics across all PSPs
- Platform-wide performance overview
- Useful for capacity planning and system health

#### **Single PSP Selection**
- Shows metrics for selected PSP only
- PSP-specific performance analysis
- Compliance and regulatory reporting

#### **Multiple PSP Selection**
- Compare metrics across selected PSPs
- Performance benchmarking
- Multi-tenant analysis

### How to Use PSP Filter (Platform Admins Only)

**Note**: PSP users do not see this dropdown. Their dashboards are automatically filtered.

1. **Locate PSP Dropdown**
   - Top of dashboard, usually in the header area
   - Labeled as "PSP" or "Payment Service Provider"
   - **Only visible to Platform Administrators**

2. **Select Filter Option**
   - Click dropdown to see available PSPs
   - Select "All PSPs" for global view
   - Select specific PSP(s) for filtered view

3. **Apply Filter**
   - Selection applies automatically
   - Dashboard refreshes with filtered data
   - All panels update to show filtered metrics

4. **Clear Filter**
   - Select "All PSPs" to clear filter
   - Dashboard returns to global view

---

## 6. Dashboard Descriptions

### 📈 **01. Transaction Overview Dashboard**

**Purpose**: Monitor transaction processing flows and decisions

**Key Metrics**:
- Total transaction rate
- Transaction decisions (allowed, blocked, held, alerted)
- Block rate percentage
- Transaction processing time
- Queue size

**PSP Filtering**: ✅ Fully PSP-segregated

**Access**:
- **PSP Users**: See only their PSP's transactions
- **Platform Admins**: Filter by PSP or view all

**Use Cases**:
- Monitor transaction processing health
- Track block rates and decision patterns
- Identify transaction volume trends
- Performance monitoring

---

### 🛡️ **02. AML Risk Dashboard**

**Purpose**: Track AML risk assessments and risk levels

**Key Metrics**:
- AML risk assessment rate
- Risk level distribution (High/Medium/Low)
- Assessment processing time (p50, p95, p99)
- Active compliance cases
- Velocity checks, geographic risk, pattern risk, amount risk

**PSP Filtering**: ✅ Fully PSP-segregated

**Access**:
- **PSP Users**: See only their PSP's AML risk metrics
- **Platform Admins**: Filter by PSP or view all

**Use Cases**:
- Monitor AML risk assessment performance
- Track high-risk transaction patterns
- Compliance case management
- Risk trend analysis

---

### 🔍 **03. Fraud Detection Dashboard**

**Purpose**: Monitor fraud detection performance and scoring

**Key Metrics**:
- Fraud assessment rate
- Fraud detection rate
- False positive rate
- Fraud score distribution
- Assessment processing time
- Risk factors (device, IP, behavioral, velocity)

**PSP Filtering**: ✅ Fully PSP-segregated

**Access**:
- **PSP Users**: See only their PSP's fraud detection metrics
- **Platform Admins**: Filter by PSP or view all

**Use Cases**:
- Monitor fraud detection accuracy
- Track false positive rates
- Analyze fraud patterns
- Model performance evaluation

---

### ⚖️ **04. Compliance Dashboard**

**Purpose**: Track compliance cases and regulatory reporting

**Key Metrics**:
- Compliance cases created/resolved/escalated
- Open cases and pending SARs
- Cases by status and priority
- SAR creation, filing, approval, rejection rates
- Case resolution time
- SAR filing time

**PSP Filtering**: ✅ Fully PSP-segregated

**Access**:
- **PSP Users**: See only their PSP's compliance metrics
- **Platform Admins**: Filter by PSP or view all

**Use Cases**:
- Compliance case management
- SAR filing tracking
- Regulatory reporting
- Case resolution monitoring

---

### ⚡ **05. System Performance Dashboard**

**Purpose**: Monitor API performance and system health

**Key Metrics**:
- API request rate
- API error rate
- API response time (p50, p95, p99)
- JVM memory usage
- Active threads
- Database connection pool
- Cache hit/miss ratio

**PSP Filtering**: ✅ API metrics PSP-filtered, infrastructure metrics global

**Access**:
- **PSP Users**: See PSP-filtered API metrics, global infrastructure metrics
- **Platform Admins**: Full access with PSP filtering

**Use Cases**:
- API performance monitoring
- System health tracking
- Resource utilization analysis
- Performance optimization

---

### 🤖 **06. Model Performance Dashboard**

**Purpose**: Monitor ML model performance and accuracy

**Key Metrics**:
- Model scoring rate (success/failure)
- Model AUC (Area Under Curve)
- Precision at 100
- Score distribution
- Scoring time (p50, p95, p99)
- Model drift score

**PSP Filtering**: ✅ Fully PSP-segregated

**Access**:
- **PSP Users**: See only their PSP's model performance
- **Platform Admins**: Filter by PSP or view all

**Use Cases**:
- Model accuracy monitoring
- Performance degradation detection
- Model drift analysis
- ML model optimization

---

### 🔎 **07. Screening Dashboard**

**Purpose**: Monitor screening operations and match rates

**Key Metrics**:
- Screening rate
- Match rate percentage
- Screening by type
- Queue size
- Screening processing time (p50, p95, p99)

**PSP Filtering**: ✅ Fully PSP-segregated

**Access**:
- **PSP Users**: See only their PSP's screening metrics
- **Platform Admins**: Filter by PSP or view all

**Use Cases**:
- Screening operation monitoring
- Match rate analysis
- Queue management
- Performance optimization

---

### 🏗️ **08. Infrastructure Resources Dashboard**

**Purpose**: Monitor core infrastructure and resource utilization

**Key Metrics**:
- JVM memory usage (heap, non-heap, direct, mapped)
- HikariCP connection pool (active, idle, max, pending)
- Garbage collection (pause time, count)
- System CPU usage
- System uptime
- Active threads

**PSP Filtering**: ❌ Global metrics (Platform Admin only)

**Access**:
- **PSP Users**: ❌ Not accessible
- **Platform Admins**: ✅ Full access

**Use Cases**:
- Infrastructure health monitoring
- Resource capacity planning
- Performance optimization
- System troubleshooting

---

### ⚙️ **09. Thread Pools Throughput Dashboard**

**Purpose**: Monitor ultra-high throughput thread pools

**Key Metrics**:
- Thread pool active threads
- Queue size
- Completed tasks
- Rejected tasks
- Overall throughput metrics

**PSP Filtering**: ❌ Global metrics (Platform Admin only)

**Access**:
- **PSP Users**: ❌ Not accessible
- **Platform Admins**: ✅ Full access

**Use Cases**:
- High-throughput performance monitoring
- Thread pool optimization
- Capacity planning
- System scalability analysis

---

### 🛡️ **10. Circuit Breaker Resilience Dashboard**

**Purpose**: Monitor Resilience4j circuit breaker metrics

**Key Metrics**:
- Circuit breaker state
- Failure rate
- Slow call rate
- Calls not permitted

**PSP Filtering**: ❌ Global metrics (Platform Admin only)

**Access**:
- **PSP Users**: ❌ Not accessible
- **Platform Admins**: ✅ Full access

**Use Cases**:
- Resilience monitoring
- Fault tolerance analysis
- System reliability tracking
- Circuit breaker optimization

---

## 7. Step-by-Step Access Instructions

### For PSP Users

#### **Step 1: Login to Grafana**

1. Navigate to Grafana URL:
   ```
   https://grafana.your-domain.com
   ```

2. Enter your PSP user credentials:
   - **Username**: Your PSP email or username
   - **Password**: Your PSP password

3. Click **"Sign In"**

#### **Step 2: Navigate to Dashboards**

1. Click **"Dashboards"** in the left sidebar
2. Select **"Browse"** to see available dashboards
3. Your PSP-specific dashboards will be listed

#### **Step 3: Access Your PSP Dashboard**

1. Click on any dashboard (e.g., "Transaction Overview")
2. Dashboard loads automatically filtered to your PSP
3. All metrics show only your PSP's data

#### **Step 4: View Metrics**

1. **Real-Time Updates**: Metrics update automatically every 10-15 seconds
2. **Time Range**: Use time picker (top right) to select time range:
   - Last 1 hour
   - Last 6 hours
   - Last 24 hours
   - Custom range

3. **Panel Interaction**:
   - Hover over graphs for detailed values
   - Click panel title to expand
   - Use zoom to focus on specific time periods

#### **Step 5: Export Data (Optional)**

1. Click panel title → **"..."** menu
2. Select **"Export CSV"** or **"Export PNG"**
3. Download for reporting or analysis

---

### For Platform Administrators

#### **Step 1: Login to Grafana**

1. Navigate to Grafana URL:
   ```
   https://grafana.your-domain.com
   ```

2. Enter platform admin credentials:
   - **Username**: Platform admin username
   - **Password**: Platform admin password

3. Click **"Sign In"**

#### **Step 2: Access All Dashboards**

1. Click **"Dashboards"** in the left sidebar
2. Select **"Browse"** to see all dashboards
3. All 10 dashboards are available

#### **Step 3: Use PSP Filtering**

1. **Global View** (Default):
   - Dashboard shows aggregated metrics across all PSPs
   - Useful for platform-wide monitoring

2. **Filter by Specific PSP**:
   - Click **"PSP"** dropdown at top of dashboard
   - Select specific PSP (e.g., "PSP_M-PESA")
   - Dashboard updates to show only that PSP's metrics

3. **Compare Multiple PSPs**:
   - Click **"PSP"** dropdown
   - Select multiple PSPs (e.g., "PSP_M-PESA" and "PSP_PAYPAL")
   - Dashboard shows metrics for selected PSPs

#### **Step 4: Monitor Platform Health**

1. **Infrastructure Dashboards**:
   - Access "Infrastructure Resources Dashboard"
   - Monitor JVM, memory, database connections
   - Track system-wide resource utilization

2. **Performance Dashboards**:
   - Access "Thread Pools Throughput Dashboard"
   - Monitor ultra-high throughput executors
   - Track system scalability

3. **Resilience Dashboards**:
   - Access "Circuit Breaker Resilience Dashboard"
   - Monitor fault tolerance
   - Track system reliability

---

## 8. Best Practices

### For PSP Users

1. **Regular Monitoring**
   - Check dashboards daily for transaction health
   - Monitor AML risk metrics weekly
   - Review compliance cases regularly

2. **Alert Configuration**
   - Set up alerts for high block rates
   - Monitor fraud detection anomalies
   - Track compliance case escalations

3. **Time Range Selection**
   - Use appropriate time ranges for analysis
   - Compare current period with historical data
   - Use custom ranges for specific investigations

4. **Data Export**
   - Export metrics for reporting
   - Save dashboard snapshots for compliance
   - Document findings for audit trails

### For Platform Administrators

1. **Multi-Tenant Monitoring**
   - Regularly review all PSPs' performance
   - Compare PSP metrics for optimization opportunities
   - Monitor platform-wide health metrics

2. **PSP Filtering Strategy**
   - Use global view for capacity planning
   - Use PSP-specific view for troubleshooting
   - Compare PSPs for performance benchmarking

3. **Infrastructure Monitoring**
   - Monitor infrastructure dashboards daily
   - Track resource utilization trends
   - Plan capacity based on aggregated metrics

4. **Alert Management**
   - Set up platform-wide alerts
   - Configure PSP-specific alerts
   - Monitor alert trends across PSPs

---

## 9. Troubleshooting

### Common Issues

#### **Issue: Cannot See PSP Data**

**Symptoms**:
- Dashboard shows "No data" or empty panels
- PSP dropdown shows no options

**Solutions**:
1. **Verify PSP Assignment**:
   - Check that your user account is assigned to a PSP
   - Contact platform administrator to verify PSP assignment

2. **Check PSP Filter**:
   - Ensure PSP dropdown is set correctly
   - Try selecting "All PSPs" and then your specific PSP

3. **Verify Metrics Collection**:
   - Check that metrics are being collected for your PSP
   - Verify Prometheus is scraping metrics correctly

#### **Issue: Dashboard Not Loading**

**Symptoms**:
- Dashboard takes long time to load
- Panels show "Loading..." indefinitely

**Solutions**:
1. **Check Network Connection**:
   - Verify network connectivity to Grafana
   - Check firewall rules

2. **Reduce Time Range**:
   - Select shorter time range (e.g., last 1 hour)
   - Large time ranges can cause slow loading

3. **Check Prometheus**:
   - Verify Prometheus is running and accessible
   - Check Prometheus metrics endpoint

#### **Issue: Incorrect PSP Filtering**

**Symptoms**:
- Seeing other PSPs' data
- PSP filter not working correctly

**Solutions**:
1. **Verify User Role**:
   - Check that you're logged in as PSP user (not platform admin)
   - PSP users should only see their own data

2. **Clear Browser Cache**:
   - Clear browser cache and cookies
   - Logout and login again

3. **Check Dashboard Configuration**:
   - Verify PSP filter is configured correctly
   - Contact platform administrator if issue persists

---

## 10. Security & Compliance

### Data Segregation

- **Complete Isolation**: Each PSP's data is completely isolated
- **No Cross-Access**: PSP users cannot access other PSPs' data
- **No PSP Dropdown**: PSP users cannot see or use PSP filter dropdown
- **Hard-Coded Filtering**: PSP user dashboards use hard-coded PSP filters
- **Audit Trails**: All dashboard access is logged for compliance

### Role-Based Access Control

**PSP Users**:
- Dashboard folders restricted to PSP-specific user groups
- PSP dropdown hidden in dashboards
- Queries hard-coded to their PSP only
- Cannot modify PSP filter

**Platform Administrators**:
- Dashboard folders restricted to Platform Admin group
- PSP dropdown visible and functional
- Queries use PSP variable for filtering
- Can view all PSPs or filter by specific PSP(s)

### Access Control

- **Role-Based Access**: Access based on user roles (PSP user vs. platform admin)
- **Authentication Required**: All dashboard access requires authentication
- **Session Management**: Automatic session timeout for security

### Compliance Features

- **Regulatory Reporting**: PSP-specific compliance metrics
- **Audit Logging**: Complete audit trails for dashboard access
- **Data Privacy**: Complete data segregation for GDPR/compliance

### Best Practices

1. **Password Security**:
   - Use strong passwords
   - Change passwords regularly
   - Never share credentials

2. **Access Management**:
   - Only grant access to authorized personnel
   - Regularly review user access
   - Revoke access for departed employees

3. **Data Handling**:
   - Export data securely
   - Protect exported data
   - Follow data retention policies

---

## 11. Quick Reference

### Dashboard URLs

```
Transaction Overview:    /d/transaction-overview
AML Risk:                /d/aml-risk
Fraud Detection:         /d/fraud-detection
Compliance:              /d/compliance
System Performance:      /d/system-performance
Model Performance:       /d/model-performance
Screening:               /d/screening
Infrastructure:          /d/infrastructure-resources
Thread Pools:            /d/thread-pools-throughput
Circuit Breaker:         /d/circuit-breaker-resilience
```

### PSP Filter Syntax

```promql
# Filter by specific PSP
{psp_code="PSP_M-PESA"}

# Filter by multiple PSPs
{psp_code=~"PSP_M-PESA|PSP_PAYPAL"}

# Filter using dashboard variable
{psp_code=~"$PSP"}
```

### Time Ranges

- **Last 5 minutes**: Real-time monitoring
- **Last 1 hour**: Recent activity
- **Last 6 hours**: Daily operations
- **Last 24 hours**: Daily summary
- **Last 7 days**: Weekly trends
- **Last 30 days**: Monthly analysis

---

## 12. Support & Contact

### For PSP Users

- **Dashboard Issues**: Contact your PSP administrator
- **Access Problems**: Contact platform support
- **Metrics Questions**: Contact compliance team

### For Platform Administrators

- **Technical Issues**: Contact DevOps team
- **Dashboard Configuration**: Contact Grafana administrator
- **Metrics Collection**: Contact Prometheus administrator

### Documentation

- **Grafana Documentation**: https://grafana.com/docs/
- **Prometheus Documentation**: https://prometheus.io/docs/
- **Internal Wiki**: [Your internal documentation URL]

---

## 13. Appendix

### A. Dashboard Refresh Rates

- **Default Refresh**: 10 seconds
- **Manual Refresh**: Click refresh button (top right)
- **Auto-Refresh**: Enabled by default

### B. Export Formats

- **CSV**: For data analysis in Excel/Google Sheets
- **PNG**: For reports and presentations
- **JSON**: For programmatic access
- **PDF**: For documentation (via Grafana reporting)

### C. Keyboard Shortcuts

- **Ctrl/Cmd + K**: Open command palette
- **Ctrl/Cmd + S**: Save dashboard
- **Ctrl/Cmd + E**: Edit dashboard
- **Ctrl/Cmd + F**: Search panels
- **Esc**: Exit fullscreen/edit mode

---

**Document Version**: 1.0  
**Last Updated**: January 2026  
**Maintained By**: Platform Operations Team

---

## Summary

This guide provides comprehensive instructions for accessing and using Grafana dashboards for PSPs and Banks. The system ensures complete multi-tenant isolation while providing powerful monitoring and analytics capabilities for both PSP users and platform administrators.

**Key Takeaways**:
- ✅ PSP users see only their own data automatically
- ✅ Platform admins can filter by PSP or view all
- ✅ 10 comprehensive dashboards cover all aspects
- ✅ Complete security and compliance features
- ✅ Real-time monitoring with 10-15 second refresh

For additional support or questions, please contact your platform administrator or refer to the internal documentation.