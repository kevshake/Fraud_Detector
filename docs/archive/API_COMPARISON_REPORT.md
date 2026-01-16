# API Comparison Report
## Frontend vs API Reference Documentation

**Generated:** 2026-01-09  
**Purpose:** Verify frontend API calls match documented backend APIs

---

## Summary

This document compares the APIs consumed by the frontend with those documented in `docs/05-API-Reference.md`.

---

## ✅ Matching Endpoints

### Dashboard & Reporting
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/reporting/summary` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/dashboard/stats` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/dashboard/transaction-volume?days={days}` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/dashboard/risk-distribution` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/dashboard/live-alerts?limit={limit}` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/dashboard/recent-transactions?limit={limit}` | Not explicitly documented | ⚠️ Needs documentation |

### Cases
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/compliance/cases` | ✅ `GET /api/v1/compliance/cases` | ✅ Match |
| `GET /api/v1/compliance/cases/{id}` | ✅ `GET /api/v1/compliance/cases/{id}` | ✅ Match |
| `GET /api/v1/cases/{id}/timeline` | ✅ `GET /api/v1/cases/{caseId}/timeline` | ✅ Match |
| `GET /api/v1/cases/{id}/network` | ✅ `GET /api/v1/cases/{caseId}/network` | ✅ Match |

### SAR Reports
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/compliance/sar` | ⚠️ `GET /api/v1/compliance/sars` (documented incorrectly) | ✅ Frontend is correct - Backend uses `/sar` |

### Alerts
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/alerts` | ✅ `GET /api/v1/alerts` | ✅ Match |

### Transactions
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/transactions?limit={limit}` | ✅ `GET /api/v1/transactions` | ✅ Match |

### Merchants
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/merchants` | ✅ `GET /api/v1/merchants` | ✅ Match |

### Users
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/users` | ✅ `GET /api/v1/users` | ✅ Match |
| `GET /api/v1/users/me` | Not explicitly documented | ⚠️ Needs documentation |

### Roles
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/roles` | Not explicitly documented | ⚠️ Needs documentation |

### Audit Logs
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/audit/logs?limit={limit}` | ⚠️ `GET /api/v1/audit-logs` (documented incorrectly) | ✅ Frontend is correct - Backend uses `/audit/logs` |

### Risk Analytics
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/analytics/risk/heatmap/{type}` | ✅ `GET /api/v1/analytics/risk/heatmap/customer` | ✅ Match (with parameter) |
| `GET /api/v1/analytics/risk/trends?days={days}` | ✅ `GET /api/v1/analytics/risk/trends` | ✅ Match |

### Compliance Calendar
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/compliance/calendar/upcoming` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/compliance/calendar/overdue` | Not explicitly documented | ⚠️ Needs documentation |

### Regulatory Reports
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/reporting/regulatory/{type}` | Not explicitly documented | ⚠️ Needs documentation |

### Transaction Monitoring
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/monitoring/dashboard/stats` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/monitoring/risk-distribution` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/monitoring/risk-indicators` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/monitoring/recent-activity` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/monitoring/transactions` | Not explicitly documented | ⚠️ Needs documentation |

### Rules Management
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/rules` | ✅ `GET /api/v1/rules` | ✅ Match |
| `GET /api/v1/rules/{id}` | Not explicitly documented | ⚠️ Needs documentation |
| `POST /api/v1/rules` | ✅ `POST /api/v1/rules` | ✅ Match |
| `PUT /api/v1/rules/{id}` | Not explicitly documented | ⚠️ Needs documentation |
| `DELETE /api/v1/rules/{id}` | ✅ `DELETE /api/v1/rules/{id}` | ✅ Match |
| `POST /api/v1/rules/{id}/enable` | Not explicitly documented | ⚠️ Needs documentation |
| `POST /api/v1/rules/{id}/disable` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/rules/{id}/effectiveness` | Not explicitly documented | ⚠️ Needs documentation |

### Limits Management
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/limits/velocity-rules` | Not explicitly documented | ⚠️ Needs documentation |
| `POST /api/v1/limits/velocity-rules` | Not explicitly documented | ⚠️ Needs documentation |
| `PUT /api/v1/limits/velocity-rules/{id}` | Not explicitly documented | ⚠️ Needs documentation |
| `DELETE /api/v1/limits/velocity-rules/{id}` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/limits/risk-thresholds` | Not explicitly documented | ⚠️ Needs documentation |
| `POST /api/v1/limits/risk-thresholds` | Not explicitly documented | ⚠️ Needs documentation |

### PSP Management
| Frontend Call | API Reference | Status |
|--------------|---------------|--------|
| `GET /api/v1/psps` | Not explicitly documented | ⚠️ Needs documentation |
| `GET /api/v1/psps/{id}` | Not explicitly documented | ⚠️ Needs documentation |

---

## ⚠️ Issues Found

### 1. API Reference Documentation Errors

#### Issue 1: SAR Reports Endpoint
- **Frontend:** `GET /api/v1/compliance/sar` ✅
- **Backend:** `GET /api/v1/compliance/sar` ✅
- **API Reference:** `GET /api/v1/compliance/sars` ❌ (incorrect - shows plural)
- **Action Required:** Update API reference documentation to use `/sar` (singular)

#### Issue 2: Audit Logs Endpoint
- **Frontend:** `GET /api/v1/audit/logs` ✅
- **Backend:** `GET /api/v1/audit/logs` ✅
- **API Reference:** `GET /api/v1/audit-logs` ❌ (incorrect - shows hyphenated)
- **Action Required:** Update API reference documentation to use `/audit/logs` (with slash)

### 2. Missing Documentation

The following endpoints are used by the frontend but not documented in the API reference:

#### Dashboard Endpoints
- `GET /api/v1/reporting/summary`
- `GET /api/v1/dashboard/stats`
- `GET /api/v1/dashboard/transaction-volume`
- `GET /api/v1/dashboard/risk-distribution`
- `GET /api/v1/dashboard/live-alerts`
- `GET /api/v1/dashboard/recent-transactions`

#### User Management
- `GET /api/v1/users/me` - Get current user
- `GET /api/v1/roles` - List roles

#### Compliance
- `GET /api/v1/compliance/calendar/upcoming`
- `GET /api/v1/compliance/calendar/overdue`

#### Reporting
- `GET /api/v1/reporting/regulatory/{type}` - CTR, LCTR, IFTR reports

#### Transaction Monitoring
- `GET /api/v1/monitoring/dashboard/stats`
- `GET /api/v1/monitoring/risk-distribution`
- `GET /api/v1/monitoring/risk-indicators`
- `GET /api/v1/monitoring/recent-activity`
- `GET /api/v1/monitoring/transactions`

#### Rules Management
- `GET /api/v1/rules/{id}` - Get single rule
- `PUT /api/v1/rules/{id}` - Update rule
- `POST /api/v1/rules/{id}/enable` - Enable rule
- `POST /api/v1/rules/{id}/disable` - Disable rule
- `GET /api/v1/rules/{id}/effectiveness` - Get rule effectiveness metrics

#### Limits Management
- `GET /api/v1/limits/velocity-rules` - List velocity rules
- `POST /api/v1/limits/velocity-rules` - Create velocity rule
- `PUT /api/v1/limits/velocity-rules/{id}` - Update velocity rule
- `DELETE /api/v1/limits/velocity-rules/{id}` - Delete velocity rule
- `GET /api/v1/limits/risk-thresholds` - List risk thresholds
- `POST /api/v1/limits/risk-thresholds` - Create/update risk threshold

#### PSP Management
- `GET /api/v1/psps` - List all PSPs
- `GET /api/v1/psps/{id}` - Get PSP by ID

---

## 📋 Recommendations

### Immediate Actions

1. **Fix API Reference Documentation:**
   - Update SAR reports endpoint from `/compliance/sars` to `/compliance/sar` (singular)
   - Update audit logs endpoint from `/audit-logs` to `/audit/logs` (with slash)

2. **Update API Reference Documentation:**
   - Add all missing endpoints listed above
   - Include request/response examples
   - Document query parameters and request bodies

3. **Verify Backend Implementation:**
   - Ensure all frontend-called endpoints exist in backend
   - Verify request/response formats match frontend expectations

### Documentation Updates Needed

Add the following sections to `docs/05-API-Reference.md`:

1. **Dashboard APIs** (Section 11)
   - Summary stats
   - Transaction volume
   - Risk distribution
   - Live alerts
   - Recent transactions

2. **User Management APIs** (Enhance Section 6)
   - Get current user (`/users/me`)
   - List roles (`/roles`)

3. **Compliance Calendar APIs** (New Section)
   - Upcoming deadlines
   - Overdue deadlines

4. **Regulatory Reporting APIs** (Enhance Section 8)
   - CTR, LCTR, IFTR report generation

5. **Transaction Monitoring APIs** (New Section)
   - Dashboard stats
   - Risk distribution
   - Risk indicators
   - Recent activity
   - Monitored transactions

6. **Rules Management APIs** (Enhance Section 15)
   - Get single rule
   - Update rule
   - Enable/disable rule
   - Rule effectiveness metrics

7. **Limits Management APIs** (New Section)
   - Velocity rules CRUD
   - Risk thresholds CRUD

8. **PSP Management APIs** (New Section)
   - List PSPs
   - Get PSP by ID

---

## ✅ Verification Checklist

- [ ] Fix SAR reports path in API reference (change `/sars` to `/sar`)
- [ ] Fix audit logs path in API reference (change `/audit-logs` to `/audit/logs`)
- [ ] Add dashboard endpoints to API reference
- [ ] Add user management endpoints to API reference
- [ ] Add compliance calendar endpoints to API reference
- [ ] Add regulatory reporting endpoints to API reference
- [ ] Add transaction monitoring endpoints to API reference
- [ ] Add rules management endpoints to API reference
- [ ] Add limits management endpoints to API reference
- [ ] Add PSP management endpoints to API reference
- [ ] Verify all endpoints exist in backend
- [ ] Test all frontend API calls against backend

---

**Note:** This report should be updated whenever new endpoints are added to either the frontend or backend.
