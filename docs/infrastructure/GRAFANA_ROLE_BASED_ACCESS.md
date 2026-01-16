# Grafana Role-Based Access Control Implementation

**Date:** January 2026  
**Version:** 1.0  
**Status:** ✅ **IMPLEMENTATION GUIDE**

---

## Overview

This document describes how to implement role-based access control in Grafana dashboards to ensure PSP users can only see their own metrics, while Platform Administrators can view all PSPs' metrics.

---

## Security Requirements

### PSP Users
- ✅ **See Only Their PSP**: Automatically filtered to their PSP's metrics
- ❌ **No PSP Dropdown**: Cannot select or view other PSPs
- ❌ **No Cross-PSP Access**: Cannot access other PSPs' data

### Platform Administrators
- ✅ **View All PSPs**: Can see all PSPs' metrics
- ✅ **PSP Dropdown**: Can filter by specific PSP(s)
- ✅ **Multi-PSP Comparison**: Can compare multiple PSPs

---

## Implementation Approaches

### Approach 1: Grafana User Context Variables (Recommended)

Use Grafana's user context and organization attributes to automatically filter PSP data.

#### Step 1: Configure Grafana User Attributes

In Grafana, configure user attributes for PSP users:

1. **For PSP Users**:
   - Set user attribute: `psp_code` = `PSP_M-PESA` (or their PSP code)
   - Set user attribute: `user_role` = `PSP_USER`

2. **For Platform Administrators**:
   - Set user attribute: `user_role` = `PLATFORM_ADMIN`
   - No `psp_code` attribute (or set to `ALL`)

#### Step 2: Update Dashboard Variables

Update dashboard variables to use user context:

```json
{
  "templating": {
    "list": [
      {
        "name": "userPSP",
        "type": "constant",
        "current": {
          "value": "${__user.psp_code}"
        },
        "hide": 2,
        "label": "User PSP"
      },
      {
        "name": "userRole",
        "type": "constant",
        "current": {
          "value": "${__user.user_role}"
        },
        "hide": 2,
        "label": "User Role"
      },
      {
        "name": "PSP",
        "type": "query",
        "query": "label_values(psp_code)",
        "hide": 0,
        "current": {
          "value": "$__all"
        },
        "includeAll": true,
        "multi": true
      }
    ]
  }
}
```

#### Step 3: Update Queries

Update PromQL queries to use conditional filtering:

```promql
# For PSP users: Use userPSP variable
# For Platform Admins: Use PSP variable
{psp_code=~"${userRole:query}${userRole:query=PSP_USER?${userPSP}:$PSP}"}
```

**Note**: Grafana doesn't support conditional variables directly. Use Approach 2 or 3.

---

### Approach 2: Custom API Endpoint (Recommended for Production)

Create a custom API endpoint that returns the user's PSP based on authentication.

#### Step 1: Create User Context API Endpoint

Create an endpoint in your Spring Boot application:

```java
@RestController
@RequestMapping("/api/v1/grafana")
public class GrafanaUserContextController {
    
    @GetMapping("/user-context")
    public Map<String, String> getUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        
        Map<String, String> context = new HashMap<>();
        
        if (user != null && user.getPsp() != null) {
            context.put("psp_code", user.getPsp().getPspCode());
            context.put("psp_id", user.getPsp().getPspId().toString());
            context.put("user_role", "PSP_USER");
        } else if (user != null && user.getRole().getName().equals("ADMIN")) {
            context.put("user_role", "PLATFORM_ADMIN");
            context.put("psp_code", "ALL");
        }
        
        return context;
    }
}
```

#### Step 2: Configure Grafana JSON Data Source

Create a JSON datasource in Grafana that queries your API:

1. Go to Configuration → Data Sources → Add data source
2. Select "JSON API"
3. Configure:
   - URL: `http://your-app:8080/api/v1/grafana/user-context`
   - Authentication: Use Grafana's proxy authentication

#### Step 3: Update Dashboard Variables

```json
{
  "name": "userPSP",
  "type": "json",
  "datasource": {
    "type": "json",
    "uid": "user-context-api"
  },
  "query": "$.psp_code",
  "hide": 2,
  "current": {
    "value": ""
  }
},
{
  "name": "userRole",
  "type": "json",
  "datasource": {
    "type": "json",
    "uid": "user-context-api"
  },
  "query": "$.user_role",
  "hide": 2,
  "current": {
    "value": ""
  }
},
{
  "name": "PSP",
  "type": "query",
  "query": "label_values(psp_code)",
  "hide": "${userRole:query}${userRole:query=PSP_USER?2:0}",
  "includeAll": true,
  "multi": true,
  "current": {
    "value": "${userRole:query}${userRole:query=PSP_USER?${userPSP}:$__all}"
  }
}
```

---

### Approach 3: Dashboard Permissions + Hidden Variables (Simplest)

Use Grafana's dashboard permissions and hidden variables.

#### Step 1: Create PSP-Specific Dashboards

Create separate dashboard copies for each PSP:
- `01-transaction-overview-psp-mpesa.json`
- `01-transaction-overview-psp-paypal.json`
- etc.

#### Step 2: Set Dashboard Permissions

1. For PSP-specific dashboards:
   - Set permissions to specific PSP user groups
   - Hide PSP dropdown (`hide: 2`)
   - Hard-code PSP filter in queries

2. For Platform Admin dashboards:
   - Set permissions to Platform Admin group
   - Show PSP dropdown (`hide: 0`)
   - Use PSP variable in queries

#### Step 3: Update Queries

**PSP User Dashboard** (hard-coded PSP):
```promql
{psp_code="PSP_M-PESA"}
```

**Platform Admin Dashboard** (variable PSP):
```promql
{psp_code=~"$PSP"}
```

---

## Recommended Implementation: Approach 2 (Custom API)

This is the most secure and flexible approach.

### Implementation Steps

1. **Create User Context Endpoint** (see code above)
2. **Configure Grafana JSON Datasource**
3. **Update All Dashboards** with role-based variables
4. **Test Access Control**

---

## Dashboard Variable Configuration

### For PSP Users

```json
{
  "name": "PSP",
  "type": "constant",
  "current": {
    "value": "${userPSP}"
  },
  "hide": 2,
  "label": "PSP (Auto-filtered)"
}
```

### For Platform Administrators

```json
{
  "name": "PSP",
  "type": "query",
  "query": "label_values(psp_code)",
  "hide": 0,
  "includeAll": true,
  "multi": true,
  "current": {
    "value": "$__all"
  }
}
```

---

## Query Updates

### PSP User Queries

```promql
# Automatically filtered to user's PSP
sum(rate(aml_transactions_total{psp_code="${userPSP}"}[5m]))
```

### Platform Admin Queries

```promql
# Filtered by selected PSP(s)
sum(rate(aml_transactions_total{psp_code=~"$PSP"}[5m]))
```

---

## Security Considerations

### 1. Authentication
- Ensure Grafana uses your application's authentication
- Use OAuth/SAML integration if available
- Verify user identity before returning PSP context

### 2. Authorization
- Validate user permissions in API endpoint
- Return only authorized PSP data
- Log all access attempts

### 3. Data Isolation
- PSP users cannot modify PSP variable
- Queries automatically filtered
- No way to bypass PSP filter

---

## Testing

### Test PSP User Access
1. Login as PSP user
2. Verify PSP dropdown is hidden
3. Verify only their PSP's data is shown
4. Verify cannot access other PSPs' data

### Test Platform Admin Access
1. Login as Platform Admin
2. Verify PSP dropdown is visible
3. Verify can select all PSPs
4. Verify can filter by specific PSP(s)

---

## Troubleshooting

### Issue: PSP Users See All PSPs

**Solution**:
1. Verify user context API returns correct PSP
2. Check dashboard variable configuration
3. Verify queries use correct variable
4. Check Grafana user attributes

### Issue: Platform Admins Cannot See PSP Dropdown

**Solution**:
1. Verify user role is set correctly
2. Check variable hide condition
3. Verify dashboard permissions
4. Check Grafana user attributes

---

## Next Steps

1. **Implement User Context API** (Approach 2)
2. **Update All Dashboards** with role-based variables
3. **Configure Grafana User Attributes**
4. **Test Access Control**
5. **Document User Setup Process**

---

## Summary

**PSP Users**:
- ✅ See only their PSP's metrics
- ❌ No PSP dropdown
- ❌ Cannot access other PSPs

**Platform Administrators**:
- ✅ See all PSPs' metrics
- ✅ PSP dropdown available
- ✅ Can filter and compare PSPs

This ensures complete data isolation for PSP users while providing Platform Administrators with full visibility.