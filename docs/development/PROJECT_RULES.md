# Project Rules & Standards

## Overview

This document defines the project-specific rules and standards for the AML Fraud Detector system. These rules complement the [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md) and must be followed by all contributors.

## Documentation Standards

### API Documentation Rule
> [!CRITICAL]
> **All API endpoints MUST be documented in `docs/05-API-Reference.md` immediately upon creation or modification.**

This is a strict rule to ensure the frontend and external consumers always have up-to-date integration details.

**Requirements:**
- Document all request/response bodies with example JSON
- Include all query parameters and path variables
- Specify authentication requirements
- Document PSP filtering behavior (Super Admin vs PSP users)
- Include error responses with HTTP status codes
- Add field descriptions for complex DTOs

**When to Update:**
- Creating a new endpoint
- Adding/removing/modifying request parameters
- Changing response structure
- Adding new error codes
- Modifying authentication/authorization requirements

### Code Component Documentation Rule
> [!IMPORTANT]
> **All new service classes, filters, and significant components MUST be documented in `docs/04-Software-Design-Document.md`.**

**Requirements:**
- Add component to package structure diagram
- Document purpose and responsibilities
- Include implementation details for complex logic
- Provide usage examples
- Document integration points with other components
- Explain design decisions and trade-offs

### Architecture Documentation Rule
> [!IMPORTANT]
> **Significant architectural changes MUST be documented in `docs/01-Technical-Architecture.md`.**

This includes:
- New technology stack components
- Changes to data flow
- New integration patterns
- Performance optimization strategies
- Security enhancements

## Code Organization Standards

### Package Structure
All code must follow the established package structure:
- `config/` - Configuration classes and filters
- `controller/` - REST API endpoints
- `service/` - Business logic
- `entity/` - JPA entities
- `repository/` - Data access
- `dto/` - Data transfer objects
- `mapper/` - MapStruct mappers
- `exception/` - Custom exceptions

### Naming Conventions

**Controllers:**
- Suffix: `Controller`
- Example: `AlertController`, `ComplianceCaseController`

**Services:**
- Suffix: `Service`
- Example: `AuditReportService`, `PspIsolationService`

**Filters:**
- Suffix: `Filter`
- Example: `PspLoggingFilter`

**DTOs:**
- Request DTOs: Suffix `Request`
- Response DTOs: Suffix `Response` or `DTO`
- Example: `ResolveAlertRequest`, `AuditReport`

## PSP Isolation Standards

### Data Access Rule
> [!CRITICAL]
> **All data access MUST respect PSP boundaries.**

**Implementation Requirements:**
- Filter all queries by PSP ID for non-Super Admin users
- Validate PSP access before returning data
- Return 403 Forbidden for cross-PSP access attempts
- Log all PSP access violations

**Example:**
```java
// Good - PSP filtering applied
if (!pspIsolationService.isPlatformAdministrator()) {
    Long userPspId = pspIsolationService.getCurrentUserPspId();
    if (!alert.getPspId().equals(userPspId)) {
        throw new ForbiddenException("Access denied");
    }
}

// Bad - No PSP validation
Alert alert = alertRepository.findById(id).orElseThrow();
return alert; // Security vulnerability!
```

### Logging Rule
> [!IMPORTANT]
> **All log statements MUST include PSP context via MDC.**

The `PspLoggingFilter` automatically injects PSP ID into MDC. Ensure your logback pattern includes `%X{pspId}`:

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [PSP:%X{pspId}] - %msg%n</pattern>
```

## Security Standards

### Authentication Rule
> [!CRITICAL]
> **All API endpoints (except public endpoints) MUST require authentication.**

Public endpoints are limited to:
- `/api/v1/auth/login`
- `/api/v1/auth/csrf`
- `/actuator/health`
- Static resources (`/css/**`, `/js/**`, `/images/**`)

### Authorization Rule
> [!CRITICAL]
> **All endpoints MUST enforce role-based access control.**

Use Spring Security annotations:
```java
@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('PSP_ADMIN')")
public ResponseEntity<?> deleteAlert(@PathVariable Long id) {
    // Implementation
}
```

### Input Validation Rule
> [!IMPORTANT]
> **All user inputs MUST be validated.**

Use Bean Validation annotations:
```java
public class CreateAlertRequest {
    @NotNull(message = "Transaction ID is required")
    private Long transactionId;
    
    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
```

## Testing Standards

### Unit Test Coverage Rule
> [!IMPORTANT]
> **All service classes MUST have unit tests with minimum 80% coverage.**

### Integration Test Rule
> [!IMPORTANT]
> **All API endpoints MUST have integration tests.**

Test scenarios must include:
- Happy path
- Validation errors
- Authentication failures
- Authorization failures (PSP isolation)
- Not found scenarios

## Performance Standards

### Query Optimization Rule
> [!IMPORTANT]
> **All database queries MUST be optimized and indexed.**

Requirements:
- Use pagination for list endpoints
- Add database indexes for frequently queried fields
- Use `@EntityGraph` or `JOIN FETCH` to avoid N+1 queries
- Monitor query performance with Hibernate statistics

### Caching Rule
> [!IMPORTANT]
> **Frequently accessed, rarely changing data MUST be cached.**

Caching layers:
1. **L1 Cache (Caffeine)**: Hot data, 5-minute TTL
2. **L2 Cache (Aerospike)**: Sanctions lists, features, 24-hour TTL
3. **Database**: Source of truth

## Error Handling Standards

### Exception Handling Rule
> [!IMPORTANT]
> **All exceptions MUST be handled by `GlobalExceptionHandler`.**

Custom exceptions should extend:
- `RuntimeException` for business logic errors
- `ResponseStatusException` for HTTP-specific errors

### Error Response Rule
> [!IMPORTANT]
> **All error responses MUST follow the standard format.**

```json
{
    "timestamp": "2026-01-16T09:00:00Z",
    "status": 400,
    "error": "Bad Request",
    "errorCode": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": ["field: error description"],
    "traceId": "abc-123-xyz"
}
```

## Audit Trail Standards

### Audit Logging Rule
> [!CRITICAL]
> **All state-changing operations MUST be logged to the audit trail.**

Operations requiring audit logs:
- User login/logout
- Case creation/update/deletion
- Alert resolution
- Merchant onboarding
- User management actions
- Configuration changes

**Implementation:**
```java
auditTrailService.log(
    action = "CASE_CREATED",
    performedBy = currentUser.getEmail(),
    merchantId = caseEntity.getMerchantId(),
    details = "Created case " + caseEntity.getCaseReference()
);
```

## Compliance Standards

### Data Retention Rule
> [!CRITICAL]
> **All data retention policies MUST be enforced.**

- Audit logs: 7 years
- Transaction data: 5 years
- Case data: 7 years
- Archived data: Move to cold storage after 1 year

### PII Protection Rule
> [!CRITICAL]
> **Personally Identifiable Information (PII) MUST be protected.**

Requirements:
- Hash PANs using SHA-256
- Encrypt sensitive fields at rest
- Mask PII in logs
- Implement data access controls

## Monitoring Standards

### Metrics Rule
> [!IMPORTANT]
> **All critical operations MUST emit metrics.**

Use Micrometer/Prometheus metrics:
```java
@Timed(value = "alert.resolution.time", description = "Time to resolve alert")
public void resolveAlert(Long alertId) {
    // Implementation
}
```

### Health Check Rule
> [!IMPORTANT]
> **All external dependencies MUST have health checks.**

Implement custom health indicators for:
- PostgreSQL
- Aerospike
- Neo4j (if used)
- External APIs

## Version Control Standards

### Commit Message Rule
> [!IMPORTANT]
> **All commits MUST have descriptive messages.**

Format:
```
[Component] Brief description

Detailed explanation of changes
- Bullet point 1
- Bullet point 2

Fixes #issue-number
```

### Branch Naming Rule
> [!IMPORTANT]
> **All branches MUST follow naming convention.**

Format: `<type>/<description>`

Types:
- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Production hotfixes
- `refactor/` - Code refactoring
- `docs/` - Documentation updates

## Review Checklist

Before submitting code for review, ensure:

- [ ] All new APIs documented in `05-API-Reference.md`
- [ ] All new components documented in `04-Software-Design-Document.md`
- [ ] PSP isolation implemented and tested
- [ ] Unit tests written with >80% coverage
- [ ] Integration tests written for all endpoints
- [ ] Error handling implemented
- [ ] Audit logging added for state changes
- [ ] Input validation implemented
- [ ] Code follows naming conventions
- [ ] No hardcoded values (use configuration)
- [ ] Recursive impact analysis performed
- [ ] All affected classes updated

---

**Last Updated:** 2026-01-16  
**Version:** 1.0
