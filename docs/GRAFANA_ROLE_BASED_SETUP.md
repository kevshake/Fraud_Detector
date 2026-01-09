# Grafana Role-Based Access Setup Guide

**Date:** January 2026  
**Version:** 1.0  
**Status:** ✅ **SETUP GUIDE**

---

## Quick Start

This guide provides step-by-step instructions to configure Grafana for role-based access control, ensuring PSP users can only see their own metrics.

---

## Prerequisites

1. Grafana installed and running
2. Prometheus datasource configured
3. User authentication configured (LDAP/OAuth/Internal)
4. User groups created in Grafana

---

## Step 1: Generate PSP-Specific Dashboards

Run the dashboard generation script:

```bash
cd /path/to/AML_FRAUD_DETECTOR
./scripts/generate-psp-dashboards.sh
```

This creates:
- `grafana/dashboards/psp-users/PSP_M-PESA/` - M-PESA specific dashboards
- `grafana/dashboards/psp-users/PSP_PAYPAL/` - PayPal specific dashboards
- `grafana/dashboards/platform-admin/` - Platform admin dashboards with PSP dropdown

---

## Step 2: Configure Grafana User Groups

### Create PSP User Groups

1. Go to **Administration → Teams**
2. Create teams for each PSP:
   - **Team Name**: `PSP_M-PESA_Users`
   - **Team Name**: `PSP_PAYPAL_Users`
   - etc.

3. Add PSP users to respective teams

### Create Platform Admin Group

1. Go to **Administration → Teams**
2. Create team:
   - **Team Name**: `Platform_Administrators`
3. Add platform admin users to this team

---

## Step 3: Configure Dashboard Folder Permissions

### For PSP Folders

1. Go to **Dashboards → Manage → Folders**
2. For each PSP folder (e.g., `PSP_M-PESA`):
   - Click **Permissions**
   - **Add Permission**:
     - **Team**: `PSP_M-PESA_Users`
     - **Permission**: **View**
   - **Remove** any other permissions

### For Platform Admin Folder

1. Go to **Dashboards → Manage → Folders**
2. For `platform-admin` folder:
   - Click **Permissions**
   - **Add Permission**:
     - **Team**: `Platform_Administrators`
     - **Permission**: **Admin** (or **View**)
   - **Remove** any other permissions

---

## Step 4: Configure User Context API Datasource

### Create JSON API Datasource

1. Go to **Configuration → Data Sources → Add data source**
2. Select **JSON API**
3. Configure:
   - **Name**: `user-context-api`
   - **URL**: `http://host.docker.internal:8080/api/v1/grafana/user-context`
   - **Authentication**: **Proxy** (uses Grafana's authentication)
   - **HTTP Method**: **GET**
   - **Timeout**: **30 seconds**
4. Click **Save & Test**

**Note**: Update URL based on your deployment:
- Docker: `http://host.docker.internal:8080`
- Kubernetes: `http://aml-fraud-detector-service:8080`
- Local: `http://localhost:8080`

---

## Step 5: Update Dashboard Provisioning

Update `grafana/dashboards/dashboard.yml`:

```yaml
apiVersion: 1

providers:
  - name: 'PSP User Dashboards'
    orgId: 1
    folder: 'PSP_M-PESA'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: false
    options:
      path: /etc/grafana/provisioning/dashboards/psp-users/PSP_M-PESA
      foldersFromFilesStructure: false

  - name: 'PSP User Dashboards - PayPal'
    orgId: 1
    folder: 'PSP_PAYPAL'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: false
    options:
      path: /etc/grafana/provisioning/dashboards/psp-users/PSP_PAYPAL
      foldersFromFilesStructure: false

  - name: 'Platform Admin Dashboards'
    orgId: 1
    folder: 'Platform Admin'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    allowUiUpdates: true
    options:
      path: /etc/grafana/provisioning/dashboards/platform-admin
      foldersFromFilesStructure: false
```

---

## Step 6: Verify Configuration

### Test PSP User Access

1. **Login as PSP User**:
   - Login with PSP user credentials
   - Verify only their PSP folder is visible
   - Verify PSP dropdown is hidden
   - Verify only their PSP's data is shown
   - Verify cannot access other PSPs' dashboards

2. **Verify Data Isolation**:
   - Check transaction metrics show only their PSP
   - Check AML risk metrics show only their PSP
   - Verify no cross-PSP data leakage

### Test Platform Admin Access

1. **Login as Platform Admin**:
   - Login with platform admin credentials
   - Verify Platform Admin folder is visible
   - Verify PSP dropdown is visible
   - Verify can select "All PSPs"
   - Verify can filter by specific PSP(s)

2. **Verify Multi-PSP Access**:
   - Select multiple PSPs
   - Verify metrics aggregate correctly
   - Verify can compare PSPs

---

## Security Checklist

- [ ] PSP users assigned to PSP-specific teams
- [ ] Platform Admins assigned to Platform Admin team
- [ ] Dashboard folders have correct permissions
- [ ] PSP dropdown hidden in PSP user dashboards
- [ ] Queries use hard-coded PSP for PSP users
- [ ] Queries use variable PSP for Platform Admins
- [ ] User context API endpoint secured
- [ ] Access logs enabled
- [ ] Dashboard provisioning configured correctly

---

## Troubleshooting

### Issue: PSP Users See All PSPs

**Solution**:
1. Verify PSP-specific dashboards are in correct folders
2. Check folder permissions are set correctly
3. Verify PSP variable is hard-coded in PSP dashboards
4. Check queries use hard-coded PSP code

### Issue: Platform Admins Cannot See PSP Dropdown

**Solution**:
1. Verify Platform Admin dashboards are in correct folder
2. Check PSP variable is query type (not constant)
3. Verify PSP variable `hide` property is `0`
4. Check dashboard provisioning configuration

### Issue: User Context API Returns 401

**Solution**:
1. Verify API endpoint is accessible
2. Check Grafana authentication is configured
3. Verify SecurityConfig allows `/api/v1/grafana/**`
4. Check user is authenticated in Grafana

---

## Maintenance

### Adding New PSP

1. Add PSP to `scripts/generate-psp-dashboards.sh`
2. Run script to generate PSP-specific dashboards
3. Create PSP user team in Grafana
4. Set folder permissions
5. Update dashboard provisioning

### Updating Dashboards

1. **For PSP Users**: Update PSP-specific dashboards in `psp-users/PSP_*/`
2. **For Platform Admins**: Update dashboards in `platform-admin/`
3. Restart Grafana or wait for auto-refresh

---

## Summary

**PSP Users**:
- ✅ Access only their PSP folder
- ✅ PSP dropdown hidden
- ✅ Queries hard-coded to their PSP
- ❌ Cannot access other PSPs

**Platform Administrators**:
- ✅ Access Platform Admin folder
- ✅ PSP dropdown visible
- ✅ Queries use PSP variable
- ✅ Can view all PSPs

This ensures complete data isolation for PSP users while providing Platform Administrators with full visibility.