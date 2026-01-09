# User Guide
## AML Fraud Detector Dashboard

**Version:** 1.0  
**Date:** January 2026

---

## 1. Getting Started

### 1.1 Accessing the System

1. Open your web browser (Chrome, Firefox, or Edge recommended)
2. Navigate to: `http://[server-address]:2637`
3. Enter your email and password
4. Click **Login**

### 1.2 First-Time Login

Upon first login, you may be prompted to:
- Change your temporary password
- Set up your profile

### 1.3 Dashboard Overview

After logging in, you'll see the main dashboard with:

```
┌─────────────────────────────────────────────────────────────┐
│  SIDEBAR              │  MAIN CONTENT AREA                  │
│  ─────────            │                                     │
│  📊 Dashboard         │  ┌─────────┐ ┌─────────┐ ┌───────┐ │
│  💳 Transactions      │  │  Stats  │ │  Stats  │ │ Stats │ │
│  🔔 Alerts            │  └─────────┘ └─────────┘ └───────┘ │
│  📁 Cases             │                                     │
│  📈 Analytics         │  ┌─────────────────────────────────┐│
│  📋 Compliance        │  │     Transaction Volume Chart    ││
│  🏪 Merchants         │  └─────────────────────────────────┘│
│  ⚙️ Settings          │                                     │
│  📝 Audit Logs        │  ┌─────────────────────────────────┐│
│                       │  │       Live Alerts Panel         ││
│                       │  └─────────────────────────────────┘│
└───────────────────────┴─────────────────────────────────────┘
```

---

## 2. Dashboard

### 2.1 Summary Statistics

The dashboard displays key metrics:

| Metric | Description |
|--------|-------------|
| **Transactions Today** | Total transactions processed today |
| **Fraud Score Avg** | Average fraud score across transactions |
| **Open Alerts** | Number of unresolved alerts |
| **Active Cases** | Cases under investigation |
| **Pending SARs** | SAR reports awaiting filing |

### 2.2 Charts

- **Transaction Volume**: Daily transaction trends over 30 days
- **Risk Breakdown**: Distribution by risk level (Low/Medium/High)
- **Fraud Detection Rate**: True positive rate over time

### 2.3 Live Alerts Panel

Shows the most recent critical alerts requiring attention:
- Click an alert to view details
- Alerts are color-coded by priority

---

## 3. Transaction Monitoring

### 3.1 Viewing Transactions

1. Click **Transactions** in the sidebar
2. Use filters to narrow results:
   - Date range
   - Merchant
   - Decision type (Block/Hold/Allow)
   - Amount range

### 3.2 Transaction Details

Click a transaction to view:
- **Basic Info**: Amount, merchant, terminal, timestamp
- **Fraud Score**: 0-100 scale with risk level
- **Decision**: BLOCK, HOLD, ALERT, or ALLOW
- **Reasons**: Why this decision was made
- **EMV Data**: Chip card information (if available)
- **Features**: ML features used for scoring

### 3.3 Searching Transactions

Use the search box to find transactions by:
- Transaction ID
- Merchant ID
- PAN (last 4 digits)

---

## 4. Alert Management

### 4.1 Alert Queue

The alert queue shows items requiring review:

| Column | Description |
|--------|-------------|
| **ID** | Alert identifier |
| **Type** | HIGH_FRAUD_SCORE, SANCTIONS_MATCH, etc. |
| **Priority** | Critical / High / Medium / Low |
| **Status** | Open / Investigating / Resolved |
| **Created** | When the alert was generated |

### 4.2 Working on Alerts

1. Click an alert to open the detail panel
2. Click **Assign to Me** to claim the alert
3. Review the transaction and supporting information
4. Make a determination:
   - **False Positive**: Not actual fraud
   - **Confirmed Fraud**: Actual fraudulent activity
   - **Escalate**: Needs further investigation (creates a case)

### 4.3 Resolving Alerts

1. Click **Resolve** button
2. Select disposition (False Positive / Confirmed Fraud)
3. Add notes explaining your decision
4. Click **Submit**

### 4.4 Bulk Operations

To handle multiple alerts:
1. Check the boxes next to alerts
2. Use the **Bulk Actions** dropdown:
   - Assign to user
   - Mark as false positive
   - Escalate to cases

---

## 5. Case Management

### 5.1 Case List

Navigate to **Cases** to see all compliance cases:

| Status | Description |
|--------|-------------|
| **NEW** | Just created, not yet assigned |
| **ASSIGNED** | Assigned to an analyst |
| **INVESTIGATING** | Active investigation |
| **PENDING REVIEW** | Awaiting supervisor review |
| **RESOLVED** | Investigation complete |
| **ESCALATED** | Escalated to SAR filing |

### 5.2 Working a Case

1. Open the case by clicking on it
2. Review the case summary:
   - Subject information
   - Related alerts
   - Transaction history
   - Risk indicators

3. Use the **Timeline** tab to see all case events

4. Add investigation notes:
   - Click **Add Note**
   - Enter your findings
   - Click **Save**

5. Attach supporting documents:
   - Click **Attach Document**
   - Upload files
   - Add description

### 5.3 Case Network Graph

The network graph shows relationships:
- Merchants connected to the subject
- Related transactions
- Other entities involved

### 5.4 Resolving Cases

1. Click **Change Status**
2. Select the appropriate resolution:
   - **Resolved - No Action**: Investigation found no issues
   - **Resolved - SAR Filed**: SAR submitted
   - **Closed**: Case closed without action
3. Add resolution summary
4. Click **Confirm**

---

## 6. SAR Reports

### 6.1 Creating a SAR

1. Navigate to **Compliance** → **SAR Reports**
2. Click **Create SAR**
3. Fill in the required fields:
   - Subject information
   - Suspicious activity type
   - Activity dates
   - Amount involved
   - Narrative description
4. Click **Save Draft**

### 6.2 SAR Workflow

```
DRAFT → REVIEW → APPROVED → FILED
```

| Status | Who Can Edit |
|--------|--------------|
| Draft | Creator |
| Review | Supervisor |
| Approved | No one (locked) |
| Filed | No one (locked) |

### 6.3 Filing a SAR

1. Open the SAR in **Approved** status
2. Click **File SAR**
3. Enter the FinCEN confirmation number
4. Click **Confirm Filing**

---

## 7. Merchant Directory

### 7.1 Viewing Merchants

The merchant directory shows all registered merchants with:
- Business name
- MCC code (category)
- Risk level
- KYC status
- Contract status

### 7.2 Merchant Details

Click a merchant to view:
- **Profile**: Business information
- **Beneficial Owners**: Ownership details
- **Transactions**: Recent transaction history
- **Risk Metrics**: Risk score and factors
- **Screening History**: AML/Sanctions screening results

### 7.3 Adding a Merchant

1. Click **Add Merchant**
2. Fill in business details
3. Add beneficial owner information
4. Submit for KYC review

---

## 8. Analytics & Reports

### 8.1 Risk Heatmap

Navigate to **Analytics** → **Risk Heatmap**:
- **Geographic View**: World map showing risk by country
- **Merchant View**: Risk distribution across merchants
- **Customer View**: Risk patterns by customer segment

### 8.2 Standard Reports

Available reports under **Compliance** → **Reports**:

| Report | Description |
|--------|-------------|
| Transaction Summary | Daily/weekly transaction statistics |
| Alert Summary | Alert disposition breakdown |
| Case Aging | Open cases by age |
| SAR Summary | SAR filing statistics |

### 8.3 Exporting Data

1. Navigate to any data table
2. Click **Export**
3. Select format (CSV or PDF)
4. Click **Download**

---

## 9. Settings

### 9.1 User Profile

Update your profile under **Settings** → **Profile**:
- Name
- Email
- Phone
- Password

### 9.2 Managing Users (Admins Only)

1. Go to **Settings** → **Users**
2. Click **Add User** to create new accounts
3. Click on a user to edit or deactivate

### 9.3 Roles & Permissions (Admins Only)

1. Go to **Settings** → **Roles**
2. View or modify role permissions
3. Custom roles can be created for specific needs

---

## 10. Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl + /` | Open search |
| `Ctrl + D` | Go to Dashboard |
| `Ctrl + T` | Go to Transactions |
| `Ctrl + A` | Go to Alerts |
| `Esc` | Close modal/dialog |

---

## 11. Troubleshooting

### 11.1 Common Issues

| Issue | Solution |
|-------|----------|
| Can't log in | Check email/password, contact admin if locked |
| Data not loading | Refresh the page, check internet connection |
| Export not working | Try a different browser, check popup blocker |
| Session expired | Log in again |

### 11.2 Getting Help

- **Email**: support@aml-system.com
- **Documentation**: See other docs in this folder
- **Admin Contact**: Contact your system administrator

---

## 12. Glossary

| Term | Definition |
|------|------------|
| **Alert** | Automated notification of suspicious activity |
| **Case** | Investigation record for compliance review |
| **SAR** | Suspicious Activity Report filed with regulators |
| **KYC** | Know Your Customer verification process |
| **MCC** | Merchant Category Code classification |
| **PAN** | Primary Account Number (card number) |
| **EDD** | Enhanced Due Diligence |

---

## Appendix: Role Permissions Matrix

| Feature | Viewer | Analyst | Officer | Admin |
|---------|--------|---------|---------|-------|
| View Dashboard | ✓ | ✓ | ✓ | ✓ |
| View Transactions | ✓ | ✓ | ✓ | ✓ |
| View Alerts | ✓ | ✓ | ✓ | ✓ |
| Resolve Alerts | | ✓ | ✓ | ✓ |
| Manage Cases | | ✓ | ✓ | ✓ |
| File SARs | | | ✓ | ✓ |
| Manage Users | | | | ✓ |
| System Settings | | | | ✓ |
