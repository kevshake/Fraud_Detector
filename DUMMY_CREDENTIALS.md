# Dummy Credentials for Testing

The following credentials have been initialized in the system (`V99__Dummy_Credentials.sql`) for testing various user roles and permissions.

**Note:** The password for ALL accounts is `password`.

| Use Case | Username | Role | Scope (PSP) | Permissions |
| :--- | :--- | :--- | :--- | :--- |
| **System Administration** | `admin` | `ADMIN` | Global (System) | All Capabilities |
| **PSP Administration** | `techflow_admin` | `ADMIN` | TechFlow (PSP) | Manage TechFlow Users, Roles |
| **Compliance Management** | `compliance` | `COMPLIANCE_OFFICER` | TechFlow (PSP) | Create/Assign/Close Cases, Approve SARs |
| **Investigation** | `investigator` | `INVESTIGATOR` | TechFlow (PSP) | View Cases, Add Notes, Draft SARs |

## How to use

1. Go to `http://localhost:8080/`.
2. Redirects to `/login.html`.
3. Enter username (e.g., `techflow_admin`) and password (`password`).
4. Access the dashboard scoped to that user's role and PSP.

### "TechFlow" PSP
A dummy PSP "TechFlow" has been created to demonstrate multi-tenancy. All users except `admin` are scoped to this PSP.
