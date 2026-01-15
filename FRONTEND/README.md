# AML Fraud Detector - React Frontend

## Overview

This is the React + TypeScript frontend for the AML Fraud Detector application. It provides a modern, responsive UI for all the functionality previously available in the combined Spring Boot application.

## Technology Stack

- **React 18** - UI library
- **TypeScript 5.2** - Type safety
- **Vite 5.0** - Build tool and dev server
- **Material-UI (MUI) 5.14** - Component library
- **TanStack Query 5.14** - Data fetching and caching
- **React Router 6.20** - Client-side routing

## Project Structure

```
FRONTEND/
├── src/
│   ├── components/
│   │   └── Layout/
│   │       ├── Sidebar.tsx       # Navigation sidebar
│   │       ├── Header.tsx        # Top header bar
│   │       └── MainLayout.tsx    # Main layout wrapper
│   ├── pages/
│   │   ├── Dashboard/            # Dashboard page
│   │   ├── Cases/                # Case management
│   │   ├── SarReports/           # SAR reports
│   │   ├── Alerts/               # Alerts management
│   │   ├── RiskAnalytics/        # Risk analytics
│   │   ├── ComplianceCalendar/   # Compliance calendar
│   │   ├── RegulatoryReports/    # Regulatory reports
│   │   ├── Merchants/            # Merchant directory
│   │   ├── TransactionMonitoring/ # Transaction monitoring
│   │   ├── Screening/            # Sanctions screening
│   │   ├── Profile/              # User profile
│   │   ├── Messages/             # Internal messages
│   │   ├── Settings/             # System settings
│   │   ├── Users/                # User management
│   │   ├── Roles/                # Role management
│   │   ├── Reports/              # Reports dashboard
│   │   ├── AuditLogs/            # Audit logs
│   │   └── LimitsAml/            # Limits & AML config
│   ├── features/
│   │   └── api/
│   │       └── queries.ts        # TanStack Query hooks
│   ├── lib/
│   │   └── apiClient.ts         # API client utility
│   ├── config/
│   │   └── api.ts               # API configuration
│   ├── types/
│   │   └── index.ts             # TypeScript types
│   ├── App.tsx                   # Main app component
│   └── main.tsx                  # Entry point
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## Available Pages

### Main Pages
1. **Dashboard** (`/dashboard`) - Overview, Analytics, Widgets
2. **Cases** (`/cases`) - Case management with sub-views:
   - All Cases
   - Timeline
   - Network Graph
   - Queues
3. **SAR Reports** (`/sar`) - Suspicious Activity Reports
4. **Alerts** (`/alerts`) - Alert management
5. **Risk Analytics** (`/risk-analytics`) - Risk heatmaps and trends
6. **Compliance Calendar** (`/compliance-calendar`) - Deadline tracking
7. **Regulatory Reports** (`/regulatory-reports`) - CTR, LCTR, IFTR
8. **Merchants** (`/merchants`) - Merchant directory
9. **Transaction Monitoring** (`/transaction-monitoring`) - With sub-views:
   - Live Monitoring
   - Analytics
   - SARs
   - Reports
10. **Screening** (`/screening`) - Sanctions screening

### User Pages
11. **Profile** (`/profile`) - User profile management
12. **Messages** (`/messages`) - Internal messaging

### Administration Pages
13. **Settings** (`/settings`) - System settings
14. **User Management** (`/users`) - User administration
15. **Role Management** (`/roles`) - Role and permission management
16. **Reports** (`/reports`) - Reporting dashboard
17. **Audit Logs** (`/audit`) - System audit trail
18. **Limits & AML** (`/limits-aml`) - AML limits configuration

## API Integration

All API calls are made through:
- **API Client** (`src/lib/apiClient.ts`) - Centralized fetch wrapper
- **TanStack Query Hooks** (`src/features/api/queries.ts`) - React hooks for data fetching
- **Base URL**: Configured in `src/config/api.ts`
- **Proxy**: Vite dev server proxies `/api/v1/**` to `http://localhost:2637`

## Running the Application

### Development
```bash
cd FRONTEND
npm install
npm run dev
```

The app will be available at `http://localhost:5173`

### Production Build
```bash
npm run build
```

Output will be in `FRONTEND/dist/`

## Features Implemented

✅ All 18 pages from the original application
✅ Responsive sidebar navigation with collapsible sections
✅ Dark theme matching the original design
✅ Material-UI components for consistent UI
✅ TanStack Query for efficient data fetching and caching
✅ TypeScript for type safety
✅ React Router for client-side navigation
✅ API integration with backend endpoints
✅ Error handling and loading states
✅ Badge notifications for counts (cases, alerts, SARs, messages)

## API Endpoints Used

The frontend integrates with the following backend endpoints:

- `GET /api/v1/reporting/summary` - Dashboard stats
- `GET /api/v1/compliance/cases` - Cases list
- `GET /api/v1/compliance/sar` - SAR reports
- `GET /api/v1/alerts` - Alerts
- `GET /api/v1/transactions` - Transactions
- `GET /api/v1/merchants` - Merchants
- `GET /api/v1/users` - Users
- `GET /api/v1/roles` - Roles
- `GET /api/v1/audit/logs` - Audit logs
- `GET /api/v1/users/me` - Current user
- `GET /api/v1/settings` - Settings
- `GET /api/v1/messages` - Messages
- `POST /api/v1/sanctions/screen` - Screening
- And many more...

## Next Steps

1. **Charts Integration**: Add Chart.js or Recharts for visualizations
2. **Forms**: Implement create/edit forms for cases, SARs, users, etc.
3. **Modals**: Add modal dialogs for detailed views
4. **Authentication**: Add login page and auth flow
5. **Real-time Updates**: Add WebSocket support for live updates
6. **Export Functionality**: Add CSV/PDF export features
7. **Advanced Filtering**: Enhance table filtering and search
8. **Network Graph**: Integrate D3.js or vis.js for network visualization

## Notes

- The frontend is configured to work with the Spring Boot backend running on port 2637
- All API calls include credentials for session-based authentication
- The UI follows the dark theme design from the original application
- All pages are functional and ready for further enhancement
