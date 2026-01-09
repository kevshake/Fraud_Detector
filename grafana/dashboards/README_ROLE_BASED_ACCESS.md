# Grafana Dashboard Role-Based Access Configuration

## Overview

This guide explains how to configure Grafana dashboards for role-based access control, ensuring PSP users can only see their own metrics while Platform Administrators can view all PSPs.

## Configuration Steps

### Step 1: Configure Grafana User Attributes

For each PSP user in Grafana:

1. Go to **Administration → Users → Select User**
2. Click **Edit** → **Attributes** tab
3. Add attribute:
   - **Key**: `psp_code`
   - **Value**: User's PSP code (e.g., `PSP_M-PESA`)
4. Add attribute:
   - **Key**: `user_role`
   - **Value**: `PSP_USER`

For Platform Administrators:

1. Go to **Administration → Users → Select User**
2. Click **Edit** → **Attributes** tab
3. Add attribute:
   - **Key**: `user_role`
   - **Value**: `PLATFORM_ADMIN`
4. Do NOT add `psp_code` attribute (or set to `ALL`)

### Step 2: Configure JSON Datasource for User Context

1. Go to **Configuration → Data Sources → Add data source**
2. Select **JSON API**
3. Configure:
   - **Name**: `user-context-api`
   - **URL**: `http://host.docker.internal:8080/api/v1/grafana/user-context`
   - **Authentication**: **Proxy** (uses Grafana's authentication)
   - **HTTP Method**: **GET**
4. Click **Save & Test**

### Step 3: Update Dashboard Variables

Each dashboard needs two variables:

1. **userPSP** (Hidden, JSON datasource):
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
   }
   ```

2. **userRole** (Hidden, JSON datasource):
   ```json
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
   }
   ```

3. **PSP** (Conditional visibility):
   ```json
   {
     "name": "PSP",
     "type": "query",
     "datasource": {
       "type": "prometheus",
       "uid": "prometheus"
     },
     "query": "label_values(psp_code)",
     "hide": 0,
     "includeAll": true,
     "multi": true,
     "current": {
       "value": "${userRole:query}${userRole:query=PSP_USER?${userPSP}:$__all}"
     }
   }
   ```

**Note**: Grafana doesn't support conditional `hide` property. Use dashboard permissions instead.

### Step 4: Alternative - Use Dashboard Permissions

Since Grafana variables can't be conditionally hidden, use dashboard permissions:

#### For PSP Users:

1. Create PSP-specific dashboard folders:
   - `PSP_M-PESA/`
   - `PSP_PAYPAL/`
   - etc.

2. Set folder permissions:
   - **PSP_M-PESA folder**: Only PSP_M-PESA user group
   - **PSP_PAYPAL folder**: Only PSP_PAYPAL user group

3. Update PSP variable in PSP-specific dashboards:
   ```json
   {
     "name": "PSP",
     "type": "constant",
     "current": {
       "value": "PSP_M-PESA"
     },
     "hide": 2
   }
   ```

#### For Platform Administrators:

1. Create **Platform Admin** dashboard folder
2. Set folder permissions: Only Platform Admin user group
3. Keep PSP dropdown visible (`hide: 0`)

### Step 5: Update Queries

#### PSP User Dashboards (Hard-coded PSP):

```promql
sum(rate(aml_transactions_total{psp_code="PSP_M-PESA"}[5m]))
```

#### Platform Admin Dashboards (Variable PSP):

```promql
sum(rate(aml_transactions_total{psp_code=~"$PSP"}[5m]))
```

## Recommended Approach: Separate Dashboard Folders

### Structure:

```
grafana/dashboards/
├── platform-admin/
│   ├── 01-transaction-overview.json (PSP dropdown visible)
│   ├── 02-aml-risk-dashboard.json
│   └── ...
├── psp-users/
│   ├── PSP_M-PESA/
│   │   ├── 01-transaction-overview.json (PSP hard-coded)
│   │   └── ...
│   ├── PSP_PAYPAL/
│   │   ├── 01-transaction-overview.json (PSP hard-coded)
│   │   └── ...
│   └── ...
```

### Benefits:

1. **Complete Isolation**: PSP users can't access other PSPs' dashboards
2. **Simple Configuration**: No complex variable logic
3. **Easy Maintenance**: Clear separation of concerns
4. **Security**: Folder-level permissions enforce access control

## Implementation Script

Create a script to generate PSP-specific dashboards:

```bash
#!/bin/bash

# List of PSPs
PSPS=("PSP_M-PESA" "PSP_PAYPAL" "PSP_STRIPE")

# List of dashboards
DASHBOARDS=("01-transaction-overview" "02-aml-risk" "03-fraud-detection" 
            "04-compliance" "05-system-performance" "06-model-performance" 
            "07-screening")

for PSP in "${PSPS[@]}"; do
    mkdir -p "grafana/dashboards/psp-users/$PSP"
    
    for DASHBOARD in "${DASHBOARDS[@]}"; do
        # Copy dashboard
        cp "grafana/dashboards/$DASHBOARD.json" "grafana/dashboards/psp-users/$PSP/"
        
        # Replace PSP variable with constant
        sed -i "s/\"name\": \"PSP\"/\"name\": \"PSP\", \"type\": \"constant\", \"current\": {\"value\": \"$PSP\"}, \"hide\": 2/g" \
            "grafana/dashboards/psp-users/$PSP/$DASHBOARD.json"
        
        # Replace queries to use hard-coded PSP
        sed -i "s/psp_code=~\"\\$PSP\"/psp_code=\"$PSP\"/g" \
            "grafana/dashboards/psp-users/$PSP/$DASHBOARD.json"
    done
done
```

## Testing

### Test PSP User Access:

1. Login as PSP user
2. Verify only their PSP folder is visible
3. Verify PSP dropdown is hidden
4. Verify only their PSP's data is shown
5. Verify cannot access other PSPs' dashboards

### Test Platform Admin Access:

1. Login as Platform Admin
2. Verify Platform Admin folder is visible
3. Verify PSP dropdown is visible
4. Verify can select all PSPs
5. Verify can filter by specific PSP(s)

## Security Checklist

- [ ] PSP users assigned to PSP-specific user groups
- [ ] Platform Admins assigned to Platform Admin group
- [ ] Dashboard folders have correct permissions
- [ ] PSP dropdown hidden in PSP user dashboards
- [ ] Queries use hard-coded PSP for PSP users
- [ ] Queries use variable PSP for Platform Admins
- [ ] User context API endpoint secured
- [ ] Access logs enabled

## Summary

**Recommended Approach**: Use separate dashboard folders with folder-level permissions.

**PSP Users**:
- Access only their PSP folder
- PSP dropdown hidden
- Queries hard-coded to their PSP

**Platform Administrators**:
- Access Platform Admin folder
- PSP dropdown visible
- Queries use PSP variable