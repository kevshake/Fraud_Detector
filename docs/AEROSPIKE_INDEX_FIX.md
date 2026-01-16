# Aerospike Index Issue - Quick Fix

**Error:** `Error 201: Index not found`  
**Service:** `AerospikeSanctionsScreeningService`  
**Status:** Application is running, but Aerospike queries are failing

---

## Root Cause

The Aerospike secondary indexes required for sanctions screening queries are missing. These should be created automatically by `AerospikeInitializationService` on startup, but they weren't.

---

## Quick Fix Options

### Option 1: Restart the Application (Recommended)

The indexes should be created on startup. Simply restart:

```bash
# Stop the application (Ctrl+C)
# Start again
mvn spring-boot:run
```

Or if using the JAR:
```bash
java -jar target/aml-fraud-detector-1.0.0-SNAPSHOT.jar
```

---

### Option 2: Create Indexes Manually via Aerospike CLI

Connect to Aerospike and create the missing indexes:

```bash
# Connect to Aerospike
aql

# Create indexes for sanctions screening
CREATE INDEX idx_metaphone ON aml.entities (name_metaphone) STRING;
CREATE INDEX idx_metaphone_alt ON aml.entities (name_metaphone_alt) STRING;
CREATE INDEX idx_entity_type ON aml.entities (entity_type) STRING;
CREATE INDEX idx_list_name ON aml.entities (list_name) STRING;

CREATE INDEX idx_pep_metaphone ON aml.pep (name_metaphone) STRING;
CREATE INDEX idx_pep_metaphone_alt ON aml.pep (name_metaphone_alt) STRING;
CREATE INDEX idx_pep_level ON aml.pep (pep_level) STRING;
CREATE INDEX idx_pep_country ON aml.pep (country) STRING;
```

---

### Option 3: Check Aerospike Service Status

Verify Aerospike is running:

```bash
# Check if Aerospike is running
asinfo -v status

# Check namespaces
asinfo -v namespaces

# List existing indexes
asinfo -v sindex
```

---

## Verification

After applying the fix, check the logs for:

```
✅ Successfully created Aerospike index: idx_metaphone
✅ Successfully created Aerospike index: idx_metaphone_alt
...
```

Or verify via AQL:
```bash
aql
> SHOW INDEXES
```

---

## Impact on Pagination

**This issue does NOT affect the pagination improvements we made today.**

The pagination changes are working correctly:
- ✅ Database migrations applied
- ✅ New columns created (risk_level, decision, psp_id)
- ✅ Indexes created
- ✅ Application started successfully
- ✅ API endpoints are accessible

The Aerospike issue only affects **sanctions screening queries**, not pagination.

---

## Long-term Fix

Consider adding retry logic or better error handling in `AerospikeInitializationService` to ensure indexes are created even if Aerospike starts after the application.

**File to check:** `AerospikeInitializationService.java` (line 135-173)
