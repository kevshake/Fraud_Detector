# Security Audit Report
**Date:** $(date)  
**Application:** AML Fraud Detector  
**Audit Scope:** Code Errors, Ethical Issues, PCI Compliance

## Executive Summary

This audit identified several issues across three categories:
- **Code Errors:** 1 critical import issue (FIXED), multiple warnings (non-critical)
- **Ethical Issues:** Country-based risk scoring (standard AML practice, properly documented)
- **PCI Compliance:** 1 critical issue (ISO message storage), 3 security concerns (hardcoded defaults)

---

## 1. CODE ERRORS

### ✅ FIXED: Missing Import in SecurityConfig
**File:** `src/main/java/com/posgateway/aml/config/SecurityConfig.java`  
**Issue:** Missing import for `CustomAuthenticationFailureHandler`  
**Status:** FIXED - Import added

### ⚠️ Non-Critical Warnings
- **390 linter warnings** across 107 files (mostly null safety warnings)
- **Unused imports** in multiple files
- **Unused variables** in some services
- **Compilation errors** in `target/generated-sources` (generated files - can be regenerated)

**Recommendation:** Address null safety warnings gradually. Generated file errors will resolve on rebuild.

---

## 2. ETHICAL ISSUES

### ✅ Country-Based Risk Scoring
**File:** `src/main/java/com/posgateway/aml/service/merchant/MerchantOnboardingService.java`  
**Lines:** 291-294

**Current Implementation:**
- High-risk countries list: `AF, IR, KP, SY, YE, MM, VE, ZW`
- Adds +20 points to risk score for merchants from these countries
- Based on international sanctions lists and AML best practices

**Ethical Assessment:**
- ✅ **COMPLIANT:** This is standard AML practice based on:
  - UN Security Council sanctions lists
  - FATF (Financial Action Task Force) high-risk jurisdictions
  - International regulatory guidance
- ✅ **Documented:** Risk scoring logic is clear and auditable
- ✅ **Transparent:** Decisions can be reviewed and appealed through case management

**Recommendations:**
1. Document the source of high-risk country list (UN/FATF)
2. Ensure merchants can appeal decisions
3. Consider making country list configurable via database (currently in properties)

### ✅ MCC-Based Risk Scoring
**File:** `src/main/java/com/posgateway/aml/service/merchant/MerchantOnboardingService.java`  
**Lines:** 296-299

**Current Implementation:**
- High-risk MCC codes: `6211, 7995, 7273, 5993, 6051`
- Adds +15 points to risk score

**Ethical Assessment:**
- ✅ **COMPLIANT:** Standard AML practice based on industry risk categories
- ✅ **Justified:** These MCCs represent industries with higher fraud/money laundering risk

---

## 3. PCI COMPLIANCE ISSUES

### 🔴 CRITICAL: ISO Message May Contain Full PAN
**File:** `src/main/java/com/posgateway/aml/service/TransactionIngestionService.java`  
**Line:** 55

**Issue:**
```java
// Store raw ISO message if provided
transaction.setIsoMsg(transactionRequest.getIsoMsg());
```

ISO 8583 messages typically contain the full Primary Account Number (PAN) in field 2. Storing raw ISO messages violates **PCI DSS Requirement 3.4**: "Render PAN unreadable anywhere it is stored."

**Impact:** HIGH - Full card numbers could be stored in database

**Current Mitigations:**
- ✅ PAN is separately hashed and stored as `panHash`
- ✅ Separate PAN field is not stored in plain text
- ❌ ISO message (which may contain PAN) is stored without masking

**Recommendation:**
1. **IMMEDIATE:** Mask PAN in ISO messages before storage
   - Parse ISO 8583 message
   - Mask field 2 (PAN) using format: `****1234` (last 4 digits only)
   - Store masked version
2. **ALTERNATIVE:** Encrypt ISO message field using AES-256-GCM
3. **ALTERNATIVE:** Don't store ISO message if not required for business logic

**Files Affected:**
- `TransactionIngestionService.java` (line 55)
- `BatchTransactionIngestionService.java` (line 74)
- `MemoryOptimizedTransactionIngestion.java` (line 62)

### ⚠️ Security Concern: Hardcoded Default Secrets
**File:** `src/main/resources/application.properties`  
**Lines:** 339, 349, 351

**Issues:**
1. **Default Encryption Key:**
   ```properties
   security.encryption.key=${ENCRYPTION_KEY:AES256DefaultKey12345678901234}
   ```
   - Default key is weak and predictable
   - Should require environment variable in production

2. **Default Password:**
   ```properties
   auth.default-password=${DEFAULT_PASSWORD:password123}
   ```
   - Weak default password
   - Should not have default in production

3. **Default Password Reset Pepper:**
   ```properties
   auth.password-reset.pepper=${PASSWORD_RESET_PEPPER:change-me-in-production}
   ```
   - Default value is a placeholder
   - Should require environment variable

**Recommendation:**
- ✅ Environment variables are supported (good)
- ⚠️ Remove defaults for production builds
- ⚠️ Add validation to fail startup if required secrets are missing in production

### ✅ PCI Compliance - Good Practices Found

1. **PAN Hashing:**
   - ✅ PAN is hashed using SHA-256 before storage
   - ✅ Only `panHash` is stored, not full PAN
   - ✅ Hash is used for velocity checks and fraud detection

2. **PII Masking:**
   - ✅ PII masking aspect for logging (`PiiMaskingAspect.java`)
   - ✅ Card numbers masked in logs (last 4 digits only)
   - ✅ Email addresses masked in logs

3. **No CVV Storage:**
   - ✅ No CVV/CVC storage found in codebase
   - ✅ Compliant with PCI DSS Requirement 3.2

4. **Encryption Service:**
   - ✅ AES-256-GCM encryption available for sensitive data
   - ✅ Encryption key configurable via environment variable

---

## 4. RECOMMENDATIONS SUMMARY

### Critical (Fix Immediately)
1. ✅ **FIXED:** Add missing import in SecurityConfig
2. 🔴 **TODO:** Mask PAN in ISO messages before storage

### High Priority
3. ⚠️ Remove default secrets from application.properties for production
4. ⚠️ Add startup validation for required environment variables

### Medium Priority
5. Address null safety warnings gradually
6. Make high-risk country list configurable via database
7. Document source of risk scoring criteria (UN/FATF)

### Low Priority
8. Clean up unused imports
9. Remove unused variables
10. Regenerate MapStruct mappers

---

## 5. COMPLIANCE STATUS

| Requirement | Status | Notes |
|------------|--------|-------|
| PCI DSS 3.2 (No CVV storage) | ✅ COMPLIANT | No CVV found |
| PCI DSS 3.4 (PAN masking) | ⚠️ PARTIAL | PAN hashed, but ISO message may contain PAN |
| PCI DSS 3.5 (Key management) | ⚠️ NEEDS REVIEW | Default key exists, env var supported |
| Ethical AI/ML | ✅ COMPLIANT | Risk scoring based on regulatory guidance |
| Data Privacy | ✅ COMPLIANT | PII masking in place |

---

## 6. NEXT STEPS

1. **Immediate:** Fix ISO message PAN masking
2. **Before Production:** Remove all default secrets
3. **Before Production:** Add environment variable validation
4. **Ongoing:** Address linter warnings
5. **Documentation:** Update security documentation with findings

---

**Report Generated:** Automated Security Audit  
**Reviewed By:** [To be filled]  
**Approved By:** [To be filled]

