# Development Rules & Guidelines

## Architecture

### Technology Stack
> [!IMPORTANT]
> **The system uses ONLY Aerospike and PostgreSQL for data storage.**

- **PostgreSQL**: Persistent storage for transactions, merchants, cases, users, and audit logs
- **Aerospike**: High-speed cache for transaction statistics, sanctions data, and velocity checks
- **NO Redis**: Redis has been completely removed from the system

## Documentation

### API Specification
> [!IMPORTANT]
> **Always update the API Document Specification (`docs/05-API-Reference.md`) whenever you make changes to API endpoints, request/response bodies, or error codes.**

This is a strict rule to ensure ensuring the frontend and external consumers always have up-to-date integration details.

-   If you add a field to a DTO, add it to the example JSON in the API Spec.
-   If you create a new endpoint, document it immediately.
-   If you deprecate a field, mark it as such in the spec.

## Frontend-Backend Integration

### Frontend Changes Require Backend APIs
> [!IMPORTANT]
> **Any change made in the frontend that requires data or functionality MUST have a corresponding backend API endpoint implemented.**

This is a strict rule to ensure complete full-stack implementation:

-   If you add a new UI feature, create the backend API endpoint(s) to support it
-   If you modify frontend data structures, update the backend DTOs and entities accordingly
-   If you add new frontend forms, implement the backend validation and persistence logic
-   Always test the complete flow from frontend to backend to database

### Data Isolation & Access Control
> [!CRITICAL]
> **Each PSP (Payment Service Provider) can ONLY see their own data. Only SUPER_ADMIN can see all data across all PSPs.**

This is a **critical security rule** that must be enforced in every API endpoint:

-   **PSP Users** (`PSP_ADMIN`, `PSP_ANALYST`, `PSP_COMPLIANCE_OFFICER`):
    -   Can only view/modify data for their own PSP
    -   Filter all queries by `pspId` from the authenticated user's context
    -   Return 403 Forbidden if attempting to access another PSP's data
    
-   **Super Admin** (`SUPER_ADMIN`):
    -   Can view all data across all PSPs
    -   No `pspId` filtering applied
    -   Full system-wide access

-   **Implementation Requirements**:
    ```java
    // Example: Always check PSP access in service layer
    @PreAuthorize("hasRole('SUPER_ADMIN') or @securityService.belongsToPsp(#pspId)")
    public List<Merchant> getMerchantsByPsp(Long pspId) {
        // Implementation
    }
    
    // Example: Filter by PSP in repository queries
    @Query("SELECT m FROM Merchant m WHERE m.pspId = :pspId OR :isSuperAdmin = true")
    List<Merchant> findByPspId(@Param("pspId") Long pspId, @Param("isSuperAdmin") boolean isSuperAdmin);
    ```

-   **Frontend Considerations**:
    -   Hide PSP selector for non-super-admin users
    -   Show only user's own PSP data by default
    -   Display "All PSPs" option only for super admins

## User Interface & Usability

### Tooltips
> [!IMPORTANT]
> **Always add tooltips for all interactive elements, form fields, buttons, icons, and any UI components throughout the application to ensure a simple and self-intuitive running system while in use.**

This is a strict rule to enhance user experience and make the application more accessible and user-friendly.

-   Add tooltips to all buttons, icons, and interactive elements.
-   Provide tooltips for form fields explaining their purpose and expected input format.
-   Include tooltips for navigation items and menu options.
-   Ensure tooltips are informative, concise, and helpful.
-   Tooltips should be consistent in style and behavior across the entire application.

## Code Changes & Dependencies

### Recursive Impact Analysis
> [!CRITICAL]
> **Always check all affected classes that are impacted after a class is edited, and recursively do so to eliminate errors after adding or modifying a feature.**

This is a **critical rule** to ensure code integrity and prevent cascading errors:

-   **When editing a class**, identify all classes that depend on it (dependents):
    -   Classes that import or extend the modified class
    -   Classes that use the modified class as a field, parameter, or return type
    -   Classes that reference methods, constants, or fields from the modified class
    -   Test classes that test the modified class

-   **Recursive checking process**:
    1.   After modifying a class, identify all directly affected classes
    2.   For each affected class, check if it has dependents that are also affected
    3.   Continue recursively until no new affected classes are found
    4.   Review and update all identified classes to ensure compatibility

-   **Verification steps**:
    -   Compile the project to catch compilation errors
    -   Run unit tests for all affected classes
    -   Check for runtime errors in integration tests
    -   Verify that API contracts remain consistent
    -   Ensure DTOs, entities, and services remain synchronized

-   **Common scenarios requiring recursive checks**:
    -   Changing method signatures (parameters, return types)
    -   Modifying class fields or properties
    -   Adding or removing methods
    -   Changing class hierarchy (inheritance, interfaces)
    -   Modifying constants or enums
    -   Updating DTOs or entity classes
    -   Changing service interfaces or implementations

## Database Configuration

### Never Disable Critical Databases
> [!CRITICAL]
> **NEVER disable Aerospike, PostgreSQL, or Neo4j in configuration properties (e.g., `aerospike.enabled=false`).**

-   All database services must remain `enabled=true` in strict compliance with production architecture.
-   For testing environments where these databases are not running, use **Mocks** (e.g., `@MockBean`) for the Client or Connection Service layers.
-   Do not use feature flags to bypass database connectivity logic; instead, mock the connectivity itself.
