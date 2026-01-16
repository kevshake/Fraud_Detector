# Grafana Dashboard Quick Reference Card

**For PSP Users & Platform Administrators**

---

## 🔐 Login

```
URL: https://grafana.your-domain.com
PSP Users: Use your PSP credentials
Platform Admins: Use admin credentials
```

---

## 📊 Available Dashboards

### PSP Users (7 Dashboards)
1. ✅ **Transaction Overview** - Transaction flows & decisions
2. ✅ **AML Risk** - Risk assessments & patterns
3. ✅ **Fraud Detection** - Fraud scoring & detection
4. ✅ **Compliance** - Cases & SAR tracking
5. ✅ **Model Performance** - ML model accuracy
6. ✅ **Screening** - Screening operations
7. ✅ **System Performance** - API metrics (PSP-filtered)

### Platform Admins (10 Dashboards)
All PSP dashboards +:
8. ✅ **Infrastructure Resources** - JVM, memory, DB
9. ✅ **Thread Pools** - High-throughput executors
10. ✅ **Circuit Breaker** - Resilience metrics

---

## 🎯 PSP Filtering

**Location**: Top of dashboard, "PSP" dropdown

**Options**:
- **All PSPs** - View all (Platform Admins only)
- **Single PSP** - View specific PSP
- **Multiple PSPs** - Compare PSPs

**How to Use**:
1. Click "PSP" dropdown
2. Select PSP(s)
3. Dashboard updates automatically

---

## ⏱️ Time Ranges

- **Last 5m** - Real-time
- **Last 1h** - Recent activity
- **Last 6h** - Daily operations
- **Last 24h** - Daily summary
- **Last 7d** - Weekly trends
- **Last 30d** - Monthly analysis

---

## 🔄 Refresh

- **Auto-Refresh**: Every 10 seconds (default)
- **Manual**: Click refresh button (top right)
- **Disable**: Click refresh dropdown → "Off"

---

## 📥 Export Data

1. Click panel title → **"..."** menu
2. Select export format:
   - **CSV** - Data analysis
   - **PNG** - Reports/presentations
   - **JSON** - Programmatic access

---

## 🚨 Common Issues

### No Data Showing
- Check PSP filter is set correctly
- Verify user has PSP assignment
- Check time range selection

### Dashboard Slow
- Reduce time range
- Check network connection
- Verify Prometheus is running

### Wrong PSP Data
- Verify user role (PSP user vs. admin)
- Clear browser cache
- Logout and login again

---

## ⌨️ Keyboard Shortcuts

- **Ctrl/Cmd + K** - Command palette
- **Ctrl/Cmd + S** - Save dashboard
- **Ctrl/Cmd + E** - Edit dashboard
- **Ctrl/Cmd + F** - Search panels
- **Esc** - Exit fullscreen/edit

---

## 📞 Support

**PSP Users**: Contact PSP administrator  
**Platform Admins**: Contact DevOps team  
**Technical Issues**: Contact Grafana admin

---

## 🔗 Dashboard URLs

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

---

**Version**: 1.0 | **Last Updated**: January 2026