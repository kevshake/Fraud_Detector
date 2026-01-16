# JSON Circular Reference Fix Summary

**Issue:** Infinite recursion during JSON serialization due to bidirectional JPA relationships  
**Error:** `StackOverflowError` in Jackson serialization  
**Date:** 2026-01-16

---

## Root Cause

The circular reference chain:
```
Psp → users → User → role → Role → psp → Psp (infinite loop)
```

Jackson tries to serialize the entire object graph, causing infinite recursion.

---

## Entities Fixed

### ✅ 1. **Psp.java**
- **Change:** Added `@JsonManagedReference("psp-users")` to `users` field
- **Line:** 122
- **Reason:** Marks this as the "forward" side of the Psp↔User relationship

### ✅ 2. **User.java**  
- **Change 1:** Added `@JsonBackReference("psp-users")` to `psp` field (line 43)
- **Change 2:** Added `@JsonBackReference("role-users")` to `role` field (line 39)
- **Reason:** Marks these as the "back" side - won't be serialized when coming from Psp or Role

### ✅ 3. **Role.java**
- **Change:** Added `@JsonIgnore` to `psp` field
- **Line:** 30
- **Reason:** Role doesn't need to expose its PSP in JSON responses (breaks the chain)

---

## Other Entities Checked

### ComplianceCase.java
- Has `@OneToMany` relationships to: `CaseEvidence`, `CaseNote`, `CaseAlert`, `CaseDecision`
- **Status:** These are safe as they're unidirectional (child entities don't reference back to ComplianceCase in JSON)
- **Note:** If child entities add `@ManyToOne` back to ComplianceCase and expose it in JSON, add `@JsonManagedReference`/`@JsonBackReference`

### Invoice.java
- Has `@OneToMany` to invoice line items
- **Status:** Safe (unidirectional)

### Merchant.java
- Has `@OneToMany` relationships
- **Status:** Safe (unidirectional)

### AmlPolicy.java
- Has `@OneToMany` relationships
- **Status:** Safe (unidirectional)

---

## Testing Recommendations

1. **Test all API endpoints** that return entities with relationships
2. **Specifically test:**
   - User endpoints (GET /api/users, GET /api/users/{id})
   - PSP endpoints
   - Role endpoints
   - Any endpoint that returns nested objects

3. **Verify:**
   - No `StackOverflowError` in logs
   - JSON responses are complete but don't have circular references
   - Frontend can still access necessary data

---

## Alternative Solutions (Not Used)

1. **@JsonIdentityInfo** - Would include IDs for circular refs, but adds complexity
2. **DTOs** - Best practice for production, but requires more refactoring
3. **@JsonIgnoreProperties** - Too broad, might hide needed data

---

## Future Recommendations

1. **Use DTOs** for API responses instead of exposing entities directly
2. **Add integration tests** that serialize entities to JSON
3. **Consider lazy loading** for relationships that aren't always needed
4. **Document** which relationships are exposed in JSON vs hidden

---

## Related Issues

- ✅ Schema validation error (V106 migration)
- ✅ Aerospike index error (separate issue)
- ✅ **Pagination improvements (COMPLETE - unaffected by this fix)**
