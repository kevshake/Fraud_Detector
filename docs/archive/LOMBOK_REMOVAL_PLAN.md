# Lombok Removal Plan

## Overview
This document tracks the removal of Lombok annotations and replacement with explicit getters/setters throughout the codebase.

## Files Using Lombok

### Entities (8 files)
1. ✅ `src/main/java/com/posgateway/aml/entity/risk/HighRiskCountry.java` - **COMPLETED**
2. ⏳ `src/main/java/com/posgateway/aml/entity/RuntimeError.java` - Needs full getters/setters + builder
3. ⏳ `src/main/java/com/posgateway/aml/entity/limits/VelocityRule.java` - Has partial getters/setters
4. ⏳ `src/main/java/com/posgateway/aml/entity/limits/RiskThreshold.java` - Has partial getters/setters
5. ⏳ `src/main/java/com/posgateway/aml/entity/limits/CountryComplianceRule.java` - Has partial getters/setters
6. ⏳ `src/main/java/com/posgateway/aml/entity/limits/MerchantTransactionLimit.java` - Has partial getters/setters
7. ⏳ `src/main/java/com/posgateway/aml/entity/limits/GlobalLimit.java` - Has partial getters/setters
8. ⏳ `src/main/java/com/posgateway/aml/entity/merchant/Merchant.java` - Check if uses Lombok

## Lombok Annotations to Remove

- `@Data` - Generates getters, setters, toString, equals, hashCode
- `@Getter` - Generates getters
- `@Setter` - Generates setters
- `@Builder` - Generates builder pattern
- `@AllArgsConstructor` - Generates all-args constructor
- `@NoArgsConstructor` - Generates no-args constructor
- `@EqualsAndHashCode` - Generates equals and hashCode
- `@ToString` - Generates toString

## Replacement Strategy

For each file:
1. Remove Lombok imports
2. Remove Lombok annotations
3. Add explicit no-args constructor (if needed)
4. Add explicit all-args constructor (if @AllArgsConstructor was present)
5. Add explicit getters for all fields
6. Add explicit setters for all fields
7. Add explicit equals() and hashCode() (if @Data or @EqualsAndHashCode was present)
8. Add explicit toString() (if @Data or @ToString was present)
9. Add explicit builder class (if @Builder was present)

## Status

- ✅ HighRiskCountry.java - Completed
- ⏳ In Progress
- ⏳ Pending

## Next Steps

1. Complete RuntimeError.java (most complex - has builder)
2. Complete remaining limit entities
3. Check Merchant.java for Lombok usage
4. Remove Lombok dependency from pom.xml
5. Verify compilation
6. Run tests
