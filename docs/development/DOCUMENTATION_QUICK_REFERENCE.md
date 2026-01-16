# Quick Documentation Reference Guide

## For Developers: When and How to Update Documentation

This guide provides quick answers to "Which `.md` file should I update?" based on what you're working on.

---

## 🎯 Quick Decision Tree

### I'm adding/modifying an API endpoint
→ **Update:** `docs/05-API-Reference.md`

**What to document:**
- Endpoint path and HTTP method
- Request body (with example JSON)
- Query parameters
- Path parameters
- Response body (with example JSON)
- PSP filtering behavior
- Error responses
- Authentication requirements

**Example:**
```markdown
### X.X Delete Alert

Delete an alert by ID with PSP access validation.

**Endpoint:** `DELETE /api/v1/alerts/{id}`

**Authentication:** Required (session-based)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| id | Long | Alert ID to delete |

**PSP Security:**
- Super Admin: Can delete any alert
- PSP users: Can only delete alerts belonging to their PSP

**Response (200 OK):**
```json
{
    "success": true,
    "message": "Alert deleted successfully"
}
```

**Error Responses:**
- `401 Unauthorized`: Not authenticated
- `403 Forbidden`: User does not have permission to delete this alert (PSP mismatch)
- `404 Not Found`: Alert not found
```

---

### I'm adding a new Service class
→ **Update:** `docs/04-Software-Design-Document.md`

**What to document:**
1. Add to package structure (Section 2.3)
2. Add component design section (Section 3.x)

**What to include:**
- Purpose and responsibilities
- Key methods with signatures
- Implementation details (for complex logic)
- Usage examples
- Integration points
- Design decisions

**Example:**
```markdown
#### 3.X.X Audit Report Service

The `AuditReportService` generates comprehensive audit reports for compliance.

**Purpose:**
- Generate regulatory audit reports
- Track user activity for compliance

**Key Methods:**
```java
public AuditReport generateAuditReport(LocalDateTime startDate, LocalDateTime endDate)
public UserActivityReport generateUserActivityReport(String userId, ...)
```

**Integration Points:**
- AuditTrailRepository: Fetches audit data
- REST Controllers: Exposed via `/api/v1/audit/reports/*`
```

---

### I'm adding a new Filter or Interceptor
→ **Update:** `docs/04-Software-Design-Document.md`

**Where:**
- Add to package structure under `config/`
- Add component documentation in Section 3 (Component Design)

**What to include:**
- Purpose
- Execution order (if relevant)
- Implementation details
- Security considerations
- Usage examples

---

### I'm adding a new Controller
→ **Update BOTH:**
1. `docs/05-API-Reference.md` - Document all endpoints
2. `docs/04-Software-Design-Document.md` - Add to package structure

---

### I'm changing the architecture (new tech, new pattern)
→ **Update:** `docs/01-Technical-Architecture.md`

**What to document:**
- New technology stack components
- Changes to data flow
- New integration patterns
- Performance optimization strategies

---

### I'm adding/modifying database entities
→ **Update:** `docs/06-Database-Design.md`

**What to document:**
- Entity structure
- Relationships
- Indexes
- Constraints

---

### I'm adding a new feature
→ **Update:** `docs/02-Functional-Specification.md`

**What to document:**
- Feature description
- User stories
- Business rules
- Acceptance criteria

---

## 📋 Documentation Checklist

Before submitting your PR, verify:

- [ ] **API Changes?** → Updated `05-API-Reference.md`
  - [ ] All endpoints documented
  - [ ] Request/response examples provided
  - [ ] PSP filtering documented
  - [ ] Error responses listed

- [ ] **New Service/Component?** → Updated `04-Software-Design-Document.md`
  - [ ] Added to package structure
  - [ ] Component design documented
  - [ ] Integration points identified

- [ ] **Architecture Change?** → Updated `01-Technical-Architecture.md`
  - [ ] New technology documented
  - [ ] Data flow updated

- [ ] **Database Change?** → Updated `06-Database-Design.md`
  - [ ] Entity documented
  - [ ] Relationships shown

- [ ] **New Feature?** → Updated `02-Functional-Specification.md`
  - [ ] Feature described
  - [ ] Business rules documented

---

## 🔍 Finding Existing Documentation

### Where is X documented?

| What you're looking for | Where to find it |
|------------------------|------------------|
| API endpoint details | `05-API-Reference.md` |
| Service class design | `04-Software-Design-Document.md` |
| Database schema | `06-Database-Design.md` |
| System architecture | `01-Technical-Architecture.md` |
| Feature specifications | `02-Functional-Specification.md` |
| Requirements | `03-Software-Requirements-Specification.md` |
| Deployment instructions | `08-Deployment-Guide.md` |
| User guide | `07-User-Guide.md` |
| Development rules | `DEVELOPMENT_RULES.md` |
| Project standards | `PROJECT_RULES.md` |

---

## 📝 Documentation Templates

### API Endpoint Template
```markdown
### X.X [Endpoint Name]

[Brief description]

**Endpoint:** `[METHOD] /api/v1/[path]`

**Authentication:** Required/Not required

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| param1 | Type | Description |

**Request Body:**
```json
{
    "field": "value"
}
```

**Response (200 OK):**
```json
{
    "field": "value"
}
```

**Error Responses:**
- `400 Bad Request`: Description
- `401 Unauthorized`: Description
- `403 Forbidden`: Description
- `404 Not Found`: Description

**PSP Filtering:**
- Super Admin: Behavior
- PSP users: Behavior
```

### Component Documentation Template
```markdown
#### X.X.X [Component Name]

The `ComponentName` [brief description].

**Purpose:**
- Purpose 1
- Purpose 2

**Implementation:**
```java
// Key code snippet
```

**Key Features:**
- Feature 1
- Feature 2

**Usage Example:**
```java
// Usage example
```

**Integration Points:**
- Component A: How it integrates
- Component B: How it integrates

**Design Decisions:**
- Decision 1: Rationale
- Decision 2: Rationale
```

---

## 🚨 Common Mistakes to Avoid

### ❌ Don't:
- Leave endpoints undocumented
- Document only in code comments
- Skip PSP filtering documentation
- Forget to document error responses
- Use outdated examples

### ✅ Do:
- Document immediately when creating/modifying
- Provide complete request/response examples
- Document PSP behavior for every endpoint
- List all possible error responses
- Keep examples up-to-date

---

## 🔗 Related Documents

- [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md) - Development standards and rules
- [PROJECT_RULES.md](PROJECT_RULES.md) - Project-specific rules and standards
- [DOCUMENTATION_UPDATE_2026-01-16.md](DOCUMENTATION_UPDATE_2026-01-16.md) - Recent documentation updates

---

## 💡 Tips for Good Documentation

1. **Be Specific**: Don't say "returns data", say "returns paginated list of alerts"
2. **Include Examples**: Always provide JSON examples for requests and responses
3. **Document Behavior**: Explain what happens in different scenarios
4. **State Assumptions**: Document any assumptions or prerequisites
5. **Keep It Current**: Update documentation when code changes
6. **Think of the Reader**: Write for someone who doesn't know the codebase

---

## 📞 Questions?

If you're unsure which file to update or how to document something:
1. Check this guide
2. Look at similar existing documentation
3. Review `PROJECT_RULES.md` and `DEVELOPMENT_RULES.md`
4. Ask the team

---

**Last Updated:** 2026-01-16  
**Version:** 1.0
