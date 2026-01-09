# Roll-Your-Own Sanctions Screening Implementation

## Overview

This document outlines the implementation of an in-house sanctions screening system that:
1. **Downloads** OFAC, UN, and EU sanctions lists daily in JSON format
2. **Stores** sanctions data in PostgreSQL database
3. **Implements** fast name-matching algorithms (Hybrid: Double Metaphone + Levenshtein)
4. **Screens** merchants and beneficial owners against sanctions, PEP, and adverse media lists

---

## Data Sources

### 1. OpenSanctions (Recommended Primary Source)

**Why OpenSanctions:**
- ✅ **Free for non-commercial use** (commercial use requires license)
- ✅ **Consolidated global sanctions** (OFAC, UN, EU, and 300+ other lists)
- ✅ **Daily updates** (some datasets every 6 hours)
- ✅ **Structured JSON format** (ready to use)
- ✅ **Well-maintained** open-source project
- ✅ **No API required** for bulk downloads

**URLs for Download:**
```
OFAC SDN List:
https://data.opensanctions.org/datasets/latest/us_ofac_sdn/targets.nested.json

UN Security Council Consolidated List:
https://data.opensanctions.org/datasets/latest/un_sc_sanctions/targets.nested.json

EU Consolidated Sanctions List:
https://data.opensanctions.org/datasets/latest/eu_fsf/targets.nested.json

All Sanctions Combined (Recommended):
https://data.opensanctions.org/datasets/latest/sanctions/targets.nested.json

Metadata (for version checking):
https://data.opensanctions.org/datasets/sanctions/latest/index.json
```

### 2. Direct Official Sources (Backup)

**OFAC:**
- URL: `https://www.treasury.gov/ofac/downloads/sdn.xml`
- Format: XML
- Updates: Daily

**UN:**
- URL: `https://scsanctions.un.org/resources/xml/en/consolidated.xml`
- Format: XML  
- Updates: As needed

**EU:**
- URL: `https://webgate.ec.europa.eu/fsd/fsf/public/files/xmlFullSanctionsList_1_1/content`
- Format: XML
- Updates: Daily

---

## Database Schema

### 1. Sanctions Lists Table

```sql
CREATE TABLE sanctions_lists (
    list_id SERIAL PRIMARY KEY,
    list_name TEXT NOT NULL,  -- e.g., 'OFAC_SDN', 'UN_SC', 'EU_FSF'
    list_source TEXT NOT NULL,  -- e.g., 'OpenSanctions', 'OFAC'
    version TEXT,  -- version/timestamp from source
    downloaded_at TIMESTAMP NOT NULL,
    record_count INT,
    metadata JSONB  -- additional metadata
);

CREATE INDEX idx_sanctions_lists_name ON sanctions_lists(list_name);
```

### 2. Sanctions Entities Table

```sql
CREATE TABLE sanctions_entities (
    entity_id BIGSERIAL PRIMARY KEY,
    list_id INT REFERENCES sanctions_lists(list_id),
    
    -- Core Identity
    full_name TEXT NOT NULL,
    aliases TEXT[],  -- array of aliases
    entity_type TEXT,  -- PERSON, ORGANIZATION, VESSEL, etc.
    
    -- Phonetic Codes (for fast matching)
    name_metaphone TEXT,  -- Double Metaphone primary code
    name_metaphone_alt TEXT,  -- Double Metaphone alternative code
    
    -- Person-Specific
    date_of_birth DATE,
    place_of_birth TEXT,
    nationality TEXT[],
    
    -- Organization-Specific
    registration_number TEXT,
    registration_country TEXT,
    
    -- Additional Data
    addresses JSONB,  -- array of address objects
    identification_docs JSONB,  -- passports, national IDs
    
    -- Sanctions Details
    sanction_type TEXT[],  -- e.g., ['OFAC', 'EU']
    program TEXT[],  -- e.g., ['SDGT', 'UKRAINE']
    listing_date DATE,
    
    -- Metadata
    source_url TEXT,
    raw_data JSONB,  -- full original JSON
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sanctions_entities_name ON sanctions_entities USING gin(to_tsvector('english', full_name));
CREATE INDEX idx_sanctions_entities_metaphone ON sanctions_entities(name_metaphone);
CREATE INDEX idx_sanctions_entities_type ON sanctions_entities(entity_type);
CREATE INDEX idx_sanctions_entities_list ON sanctions_entities(list_id);
CREATE INDEX idx_sanctions_entities_aliases ON sanctions_entities USING gin(aliases);
```

### 3. PEP (Politically Exposed Persons) Table

```sql
CREATE TABLE pep_entities (
    pep_id BIGSERIAL PRIMARY KEY,
    list_id INT REFERENCES sanctions_lists(list_id),
    
    -- Core Identity
    full_name TEXT NOT NULL,
    aliases TEXT[],
    
    -- Phonetic Codes
    name_metaphone TEXT,
    name_metaphone_alt TEXT,
    
    -- PEP Details
    pep_level TEXT,  -- CURRENT, FORMER, RCA (Relative/Close Associate)
    position TEXT,
    organization TEXT,
    country TEXT,
    start_date DATE,
    end_date DATE,
    
    -- Person Details
    date_of_birth DATE,
    nationality TEXT[],
    
    -- Metadata
    raw_data JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pep_entities_name ON pep_entities USING gin(to_tsvector('english', full_name));
CREATE INDEX idx_pep_entities_metaphone ON pep_entities(name_metaphone);
CREATE INDEX idx_pep_entities_level ON pep_entities(pep_level);
CREATE INDEX idx_pep_entities_country ON pep_entities(country);
```

---

## Name-Matching Algorithms

### Recommended Approach: Hybrid (Double Metaphone + Levenshtein)

Based on research, the optimal approach combines:
1. **Double Metaphone** - Fast phonetic pre-filtering
2. **Levenshtein Distance** - Precise similarity scoring

### Performance Comparison

| Algorithm | Speed | Accuracy | Best For |
|-----------|-------|----------|----------|
| **Soundex** | Fast | Low (high false positives) | Simple Western names only |
| **Metaphone** | Fast | Medium | English names |
| **Double Metaphone** | Fast | High | International names, diverse spellings |
| **Levenshtein** | Slow (O(n*m)) | High | Typos, spelling variations |
| **Hybrid (Metaphone + Levenshtein)** | **Fast + Accurate** | **Best** | **Production use** |

### Why Hybrid Approach?

1. **Stage 1: Double Metaphone (Fast Pre-filter)**
   - Convert query name to metaphone codes
   - Filter candidates using indexed metaphone columns
   - Reduces search space by 90%+
   - Time complexity: O(1) for index lookup

2. **Stage 2: Levenshtein (Precise Scoring)**
   - Calculate edit distance for metaphone matches
   - Apply threshold (e.g., distance ≤ 3)
   - Rank results by similarity score
   - Time complexity: O(n*m) only for small candidate set

### Java Implementation

**Dependencies (Maven):**
```xml
<!-- Apache Commons Text for Levenshtein -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-text</artifactId>
    <version>1.11.0</version>
</dependency>

<!-- Apache Commons Codec for Double Metaphone -->
<dependency>
    <groupId>commons-codec</groupId>
    <artifactId>commons-codec</artifactId>
    <version>1.16.0</version>
</dependency>
```

**Name Matching Service:**
```java
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.text.similarity.LevenshteinDistance;

@Service
public class NameMatchingService {
    
    private final DoubleMetaphone doubleMetaphone = new DoubleMetaphone();
    private final LevenshteinDistance levenshteinDistance = new LevenshteinDistance(5); // threshold = 5
    
    @Value("${sanctions.matching.levenshtein.threshold:3}")
    private int levenshteinThreshold;
    
    @Value("${sanctions.matching.similarity.threshold:0.8}")
    private double similarityThreshold;
    
    /**
     * Generate phonetic codes for a name
     */
    public PhoneticCodes generatePhoneticCodes(String name) {
        String normalized = normalizeName(name);
        String primaryCode = doubleMetaphone.doubleMetaphone(normalized);
        String alternateCode = doubleMetaphone.doubleMetaphone(normalized, true);
        
        return new PhoneticCodes(primaryCode, alternateCode);
    }
    
    /**
     * Calculate similarity score between two names
     * Returns score from 0.0 (no match) to 1.0 (exact match)
     */
    public double calculateSimilarity(String name1, String name2) {
        String normalized1 = normalizeName(name1);
        String normalized2 = normalizeName(name2);
        
        // Calculate Levenshtein distance
        int distance = levenshteinDistance.apply(normalized1, normalized2);
        
        // Convert to similarity score (0.0 to 1.0)
        int maxLength = Math.max(normalized1.length(), normalized2.length());
        return 1.0 - ((double) distance / maxLength);
    }
    
    /**
     * Check if two names match based on thresholds
     */
    public boolean isMatch(String name1, String name2) {
        // Exact match (fast path)
        if (normalizeName(name1).equals(normalizeName(name2))) {
            return true;
        }
        
        // Phonetic match check
        PhoneticCodes codes1 = generatePhoneticCodes(name1);
        PhoneticCodes codes2 = generatePhoneticCodes(name2);
        
        if (!codes1.matchesAny(codes2)) {
            return false; // No phonetic match, skip Levenshtein
        }
        
        // Calculate similarity for phonetic matches
        double similarity = calculateSimilarity(name1, name2);
        return similarity >= similarityThreshold;
    }
    
    /**
     * Normalize name for comparison
     */
    private String normalizeName(String name) {
        if (name == null) return "";
        
        return name.trim()
                   .toLowerCase()
                   .replaceAll("[^a-z0-9\\s]", "") // Remove special chars
                   .replaceAll("\\s+", " "); // Normalize whitespace
    }
    
    @Data
    @AllArgsConstructor
    public static class PhoneticCodes {
        private String primaryCode;
        private String alternateCode;
        
        public boolean matchesAny(PhoneticCodes other) {
            return (this.primaryCode != null && this.primaryCode.equals(other.primaryCode)) ||
                   (this.primaryCode != null && this.primaryCode.equals(other.alternateCode)) ||
                   (this.alternateCode != null && this.alternateCode.equals(other.primaryCode)) ||
                   (this.alternateCode != null && this.alternateCode.equals(other.alternateCode));
        }
    }
}
```

---

## Daily List Download Service

### Implementation

```java
@Service
@Slf4j
public class SanctionsListDownloadService {
    
    @Value("${sanctions.download.enabled:true}")
    private boolean downloadEnabled;
    
    @Value("${sanctions.download.source:opensanctions}")
    private String downloadSource;
    
    @Value("${sanctions.opensanctions.url:https://data.opensanctions.org/datasets/latest/sanctions/targets.nested.json}")
    private String openSanctionsUrl;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private SanctionsRepository sanctionsRepository;
    
    @Autowired
    private NameMatchingService nameMatchingService;
    
    /**
     * Scheduled job to download sanctions lists daily at 2 AM
     */
    @Scheduled(cron = "${sanctions.download.cron:0 0 2 * * *}")
    public void downloadDailySanctionsList() {
        if (!downloadEnabled) {
            log.info("Sanctions list download is disabled");
            return;
        }
        
        log.info("Starting daily sanctions list download");
        
        try {
            // 1. Check if new version available
            String metadataUrl = openSanctionsUrl.replace("targets.nested.json", "index.json");
            SanctionsMetadata metadata = fetchMetadata(metadataUrl);
            
            if (isAlreadyDownloaded(metadata.getVersion())) {
                log.info("Sanctions list already up to date. Version: {}", metadata.getVersion());
                return;
            }
            
            // 2. Download full list
            SanctionsData data = downloadSanctionsList(openSanctionsUrl);
            
            // 3. Parse and store in database
            int recordCount = processSanctionsData(data, metadata);
            
            log.info("Successfully downloaded and processed {} sanctions records", recordCount);
            
        } catch (Exception e) {
            log.error("Error downloading sanctions list", e);
        }
    }
    
    private SanctionsMetadata fetchMetadata(String url) {
        return restTemplate.getForObject(url, SanctionsMetadata.class);
    }
    
    private SanctionsData downloadSanctionsList(String url) {
        return restTemplate.getForObject(url, SanctionsData.class);
    }
    
    private boolean isAlreadyDownloaded(String version) {
        return sanctionsRepository.existsByVersion(version);
    }
    
    @Transactional
    private int processSanctionsData(SanctionsData data, SanctionsMetadata metadata) {
        // 1. Create new list entry
        SanctionsList list = new SanctionsList();
        list.setListName("OPENSANCTIONS_ALL");
        list.setListSource("OpenSanctions");
        list.setVersion(metadata.getVersion());
        list.setDownloadedAt(LocalDateTime.now());
        list.setRecordCount(data.getTargets().size());
        list = sanctionsRepository.saveList(list);
        
        // 2. Process entities in batches
        int batchSize = 1000;
        int count = 0;
        List<SanctionsEntity> batch = new ArrayList<>();
        
        for (SanctionsTarget target : data.getTargets()) {
            SanctionsEntity entity = convertToEntity(target, list.getListId());
            batch.add(entity);
            
            if (batch.size() >= batchSize) {
                sanctionsRepository.batchInsertEntities(batch);
                count += batch.size();
                batch.clear();
                log.info("Processed {} sanctions records", count);
            }
        }
        
        // Save remaining records
        if (!batch.isEmpty()) {
            sanctionsRepository.batchInsertEntities(batch);
            count += batch.size();
        }
        
        return count;
    }
    
    private SanctionsEntity convertToEntity(SanctionsTarget target, Long listId) {
        SanctionsEntity entity = new SanctionsEntity();
        entity.setListId(listId);
        entity.setFullName(target.getName());
        entity.setAliases(target.getAliases());
        entity.setEntityType(target.getSchema());
        
        // Generate phonetic codes
        PhoneticCodes codes = nameMatchingService.generatePhoneticCodes(target.getName());
        entity.setNameMetaphone(codes.getPrimaryCode());
        entity.setNameMetaphoneAlt(codes.getAlternateCode());
        
        // Set other fields...
        entity.setDat of_birth(target.getBirthDate());
        entity.setNationality(target.getNationalities());
        entity.setRawData(target.toJson());
        
        return entity;
    }
}
```

---

## Sanctions Screening Service

```java
@Service
@Slf4j
public class SanctionsScreeningService {
    
    @Autowired
    private SanctionsRepository sanctionsRepository;
    
    @Autowired
    private NameMatchingService nameMatchingService;
    
    @Value("${sanctions.screening.min.similarity:0.8}")
    private double minSimilarity;
    
    /**
     * Screen a name against all sanctions lists
     */
    public ScreeningResult screenName(String name, EntityType entityType) {
        log.info("Screening name: {} (type: {})", name, entityType);
        
        // 1. Generate phonetic codes
        PhoneticCodes codes = nameMatchingService.generatePhoneticCodes(name);
        
        // 2. Find candidates using phonetic match (fast)
        List<SanctionsEntity> candidates = sanctionsRepository
            .findByPhoneticCodes(codes.getPrimaryCode(), codes.getAlternateCode());
        
        log.debug("Found {} phonetic candidates for: {}", candidates.size(), name);
        
        // 3. Calculate similarity scores (precise)
        List<SanctionsMatch> matches = new ArrayList<>();
        for (SanctionsEntity candidate : candidates) {
            double similarity = nameMatchingService.calculateSimilarity(name, candidate.getFullName());
            
            if (similarity >= minSimilarity) {
                matches.add(new SanctionsMatch(candidate, similarity, "NAME_MATCH"));
            }
            
            // Check aliases
            if (candidate.getAliases() != null) {
                for (String alias : candidate.getAliases()) {
                    double aliasSimilarity = nameMatchingService.calculateSimilarity(name, alias);
                    if (aliasSimilarity >= minSimilarity) {
                        matches.add(new SanctionsMatch(candidate, aliasSimilarity, "ALIAS_MATCH"));
                    }
                }
            }
        }
        
        // 4. Sort by similarity score (highest first)
        matches.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        
        // 5. Determine screening status
        ScreeningStatus status = matches.isEmpty() ? ScreeningStatus.CLEAR : 
                                 matches.get(0).getSimilarity() >= 0.95 ? ScreeningStatus.MATCH :
                                 ScreeningStatus.POTENTIAL_MATCH;
        
        log.info("Screening result for '{}': {} ({} matches)", name, status, matches.size());
        
        return new ScreeningResult(name, status, matches);
    }
    
    /**
     * Screen merchant entity
     */
    public ScreeningResult screenMerchant(String legalName, String tradingName) {
        // Screen both legal and trading names
        ScreeningResult legalResult = screenName(legalName, EntityType.ORGANIZATION);
        
        ScreeningResult tradingResult = null;
        if (tradingName != null && !tradingName.equals(legalName)) {
            tradingResult = screenName(tradingName, EntityType.ORGANIZATION);
        }
        
        // Combine results
        return combineResults(legalResult, tradingResult);
    }
    
    /**
     * Screen beneficial owner
     */
    public ScreeningResult screenBeneficialOwner(String fullName, LocalDate dateOfBirth) {
        ScreeningResult nameResult = screenName(fullName, EntityType.PERSON);
        
        // Additional DOB matching for higher confidence
        if (!nameResult.getMatches().isEmpty() && dateOfBirth != null) {
            nameResult.getMatches().forEach(match -> {
                if (match.getEntity().getDateOfBirth() != null) {
                    if (match.getEntity().getDateOfBirth().equals(dateOfBirth)) {
                        match.setSimilarity(Math.min(1.0, match.getSimilarity() + 0.1)); // Boost score
                        match.setMatchType(match.getMatchType() + "_DOB_CONFIRMED");
                    }
                }
            });
            
            // Re-sort after boosting
            nameResult.getMatches().sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        }
        
        return nameResult;
    }
}
```

---

## Configuration Properties

Add to `application.properties`:

```properties
# Sanctions Screening Configuration
sanctions.download.enabled=true
sanctions.download.source=opensanctions
sanctions.download.cron=0 0 2 * * *
sanctions.opensanctions.url=https://data.opensanctions.org/datasets/latest/sanctions/targets.nested.json

# Name Matching Configuration
sanctions.matching.levenshtein.threshold=3
sanctions.matching.similarity.threshold=0.8
sanctions.screening.min.similarity=0.8

# Cache Configuration (for screening results)
sanctions.cache.enabled=true
sanctions.cache.ttl.hours=24
```

---

## Performance Optimization

### 1. Database Indexing
- ✅ GIN index on `full_name` (full-text search)
- ✅ B-tree index on `name_metaphone` (phonetic lookup)
- ✅ Array index on `aliases`
- ✅ Partial indexes on entity_type, list_id

### 2. Caching Strategy
```java
@Cacheable(value = "sanctionsScreening", key = "#name")
public ScreeningResult screenName(String name, EntityType entityType) {
    // ... screening logic
}
```

### 3. Batch Processing
- Process downloads in batches of 1,000 records
- Use JDBC batch inserts for better performance

### 4. Expected Performance
- **Phonetic pre-filtering:** <10ms (indexed query)
- **Levenshtein calculation:** 10-50ms (for 10-100 candidates)
- **Total screening time:** 20-60ms per name
- **Throughput:** 1,000+ screenings/second (single instance)

---

## Integration with Merchant Onboarding

```java
@Service
public class MerchantOnboardingService {
    
    @Autowired
    private SanctionsScreeningService sanctionsScreeningService;
    
    @Autowired
    private MerchantRiskScoringService riskScoringService;
    
    @Transactional
    public MerchantOnboardingResult onboardMerchant(MerchantApplication application) {
        // 1. Screen merchant name
        ScreeningResult merchantScreening = sanctionsScreeningService
            .screenMerchant(application.getLegalName(), application.getTradingName());
        
        // 2. Screen all beneficial owners
        List<ScreeningResult> ownerScreenings = new ArrayList<>();
        for (BeneficialOwner owner : application.getBeneficialOwners()) {
            ScreeningResult ownerScreening = sanctionsScreeningService
                .screenBeneficialOwner(owner.getFullName(), owner.getDateOfBirth());
            ownerScreenings.add(ownerScreening);
        }
        
        // 3. Calculate risk score
        MerchantRiskScore riskScore = riskScoringService
            .calculateRiskScore(merchantScreening, ownerScreenings, application);
        
        // 4. Make decision
        OnboardingDecision decision = makeDecision(riskScore, merchantScreening, ownerScreenings);
        
        // 5. Create compliance case if needed
        if (decision == OnboardingDecision.REVIEW || decision == OnboardingDecision.REJECT) {
            createComplianceCase(application, merchantScreening, ownerScreenings, decision);
        }
        
        return new MerchantOnboardingResult(decision, riskScore, merchantScreening, ownerScreenings);
    }
}
```

---

## Testing Strategy

### 1. Unit Tests
- Test name normalization
- Test phonetic code generation
- Test Levenshtein calculation
- Test similarity scoring

### 2. Integration Tests
- Test download service with mock data
- Test database storage and retrieval
- Test screening with known matches

### 3. Performance Tests
- Benchmark screening with 10,000+ sanctions records
- Measure phonetic vs full Levenshtein performance
- Load test with concurrent screening requests

---

## Rule Engine Implementation

### Recommended: Easy Rules (Lightweight & Simple)

**Why Easy Rules over Drools:**
- ✅ Lightweight (no complex setup)
- ✅ Simple YAML/Java DSL
- ✅ Fast execution
- ✅ Easy to unit test
- ✅ Perfect for payment rules

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.jeasy</groupId>
    <artifactId>easy-rules-core</artifactId>
    <version>4.1.0</version>
</dependency>
<dependency>
    <groupId>org.jeasy</groupId>
    <artifactId>easy-rules-mvel</artifactId>
    <version>4.1.0</version>
</dependency>
```

### Risk Scoring Rules (YAML Configuration)

```yaml
# config/risk-rules.yml
---
name: "High Risk Country"
description: "Merchant operating in high-risk country"
priority: 1
condition: "merchant.country in ['AF', 'IR', 'KP', 'SY', 'YE']"
actions:
  - "riskScore.addPoints(50)"
  - "riskScore.addReason('High-risk country: ' + merchant.country)"
---
name: "High Risk Industry"
description: "High-risk MCC code"
priority: 2
condition: "merchant.mcc in ['6211', '7995', '7273']"  # Money transfer, gambling, dating
actions:
  - "riskScore.addPoints(30)"
  - "riskScore.addReason('High-risk industry: MCC ' + merchant.mcc)"
---
name: "Sanctions Match"
description: "Merchant or UBO matched sanctions list"
priority: 10  # Highest priority
condition: "screeningResult.status == 'MATCH'"
actions:
  - "riskScore.addPoints(100)"
  - "riskScore.setDecision('REJECT')"
  - "riskScore.addReason('Sanctions match detected')"
---
name: "PEP Match"
description: "Beneficial owner is PEP"
priority: 8
condition: "pepScreeningResult.status == 'MATCH' && pepScreeningResult.level == 'CURRENT'"
actions:
  - "riskScore.addPoints(40)"
  - "riskScore.requireEDD(true)"
  - "riskScore.addReason('Current PEP detected')"
---
name: "High Transaction Volume"
description: "Expected monthly volume exceeds threshold"
priority: 3
condition: "merchant.expectedMonthlyVolume > 1000000"  # $1M+
actions:
  - "riskScore.addPoints(20)"
  - "riskScore.requireEDD(true)"
  - "riskScore.addReason('High transaction volume: $' + merchant.expectedMonthlyVolume)"
---
name: "New Business"
description: "Business registered less than 6 months ago"
priority: 4
condition: "merchant.registrationDate.isAfter(now().minusMonths(6))"
actions:
  - "riskScore.addPoints(15)"
  - "riskScore.addReason('New business (less than 6 months)')"
---
name: "Shell Company Indicators"
description: "No website or physical address"
priority: 5
condition: "(merchant.website == null || merchant.website.isEmpty()) && merchant.physicalAddress == null"
actions:
  - "riskScore.addPoints(25)"
  - "riskScore.addReason('Shell company indicator: no web presence or physical address')"
---
name: "Cross Border Operations"
description: "Merchant operates in multiple high-risk countries"
priority: 6
condition: "merchant.operatingCountries.size() > 3 && merchant.hasHighRiskCountries()"
actions:
  - "riskScore.addPoints(20)"
  - "riskScore.addReason('Cross-border operations in high-risk jurisdictions')"
```

### Rule Engine Service

```java
@Service
public class RiskRulesEngine {
    
    private RulesEngine rulesEngine;
    
    @PostConstruct
    public void init() {
        // Load rules from YAML
        RulesEngineParameters parameters = new RulesEngineParameters()
            .skipOnFirstAppliedRule(false)  // Apply all matching rules
            .skipOnFirstFailedRule(false)
            .skipOnFirstNonTriggeredRule(false);
        
        rulesEngine = new DefaultRulesEngine(parameters);
    }
    
    public RiskScore evaluateRisk(MerchantApplication merchant, 
                                   ScreeningResult screeningResult,
                                   ScreeningResult pepScreeningResult) {
        
        RiskScore riskScore = new RiskScore();
        
        // Create facts
        Facts facts = new Facts();
        facts.put("merchant", merchant);
        facts.put("screeningResult", screeningResult);
        facts.put("pepScreeningResult", pepScreeningResult);
        facts.put("riskScore", riskScore);
        facts.put("now", LocalDateTime::now);
        
        // Load and fire rules
        Rules rules = loadRulesFromYaml("config/risk-rules.yml");
        rulesEngine.fire(rules, facts);
        
        // Calculate final decision
        riskScore.calculateFinalScore();
        
        return riskScore;
    }
}

@Data
public class RiskScore {
    private int totalPoints = 0;
    private List<String> reasons = new ArrayList<>();
    private OnboardingDecision decision;
    private boolean requireEDD = false;
    
    public void addPoints(int points) {
        this.totalPoints += points;
    }
    
    public void addReason(String reason) {
        this.reasons.add(reason);
    }
    
    public void calculateFinalScore() {
        if (totalPoints >= 80) {
            decision = OnboardingDecision.REJECT;
        } else if (totalPoints >= 50 || requireEDD) {
            decision = OnboardingDecision.ENHANCED_DUE_DILIGENCE;
        } else if (totalPoints >= 30) {
            decision = OnboardingDecision.REVIEW;
        } else {
            decision = OnboardingDecision.APPROVE;
        }
    }
}
```

### Configurable Thresholds (Database-Driven)

```java
// Store in model_config table
INSERT INTO model_config (config_key, value, description) VALUES
('risk.threshold.approve', '30', 'Risk score threshold for auto-approval'),
('risk.threshold.review', '50', 'Risk score threshold for manual review'),
('risk.threshold.edd', '50', 'Risk score threshold for enhanced due diligence'),
('risk.threshold.reject', '80', 'Risk score threshold for auto-rejection'),
('risk.country.high_risk', 'AF,IR,KP,SY,YE,MM', 'High-risk country codes'),
('risk.mcc.high_risk', '6211,7995,7273,5993', 'High-risk MCC codes'),
('risk.volume.threshold', '1000000', 'High volume threshold (cents)'),
('risk.business.new_months', '6', 'Months to consider business as new');
```

---

## Periodic Rescreening Implementation

### Scheduled Rescreening Service

```java
@Service
@Slf4j
public class PeriodicRescreeningService {
    
    @Autowired
    private MerchantRepository merchantRepository;
    
    @Autowired
    private SanctionsScreeningService screeningService;
    
    @Value("${rescreening.frequency.days:30}")
    private int rescreeningFrequencyDays;
    
    /**
     * Scheduled job: Rescreen all active merchants monthly
     * Runs at 3 AM on the 1st of each month
     */
    @Scheduled(cron = "${rescreening.cron:0 0 3 1 * *}")
    public void rescreenAllMerchants() {
        log.info("Starting periodic merchant rescreening");
        
        LocalDate cutoffDate = LocalDate.now().minusDays(rescreeningFrequencyDays);
        
        // Find merchants needing rescreening
        List<Merchant> merchants = merchantRepository
            .findActiveNotScreenedSince(cutoffDate);
        
        log.info("Found {} merchants needing rescreening", merchants.size());
        
        int rescreened = 0;
        int newMatches = 0;
        
        for (Merchant merchant : merchants) {
            try {
                RescreeningResult result = rescreenMerchant(merchant);
                rescreened++;
                
                if (result.hasNewMatches()) {
                    newMatches++;
                    createMonitoringAlert(merchant, result);
                }
                
            } catch (Exception e) {
                log.error("Error rescreening merchant: {}", merchant.getMerchantId(), e);
            }
        }
        
        log.info("Rescreening completed: {} merchants rescreened, {} new matches", 
                 rescreened, newMatches);
    }
    
    private RescreeningResult rescreenMerchant(Merchant merchant) {
        // Screen merchant name
        ScreeningResult currentResult = screeningService
            .screenMerchant(merchant.getLegalName(), merchant.getTradingName());
        
        // Screen all UBOs
        List<ScreeningResult> uboResults = merchant.getBeneficialOwners()
            .stream()
            .map(ubo -> screeningService.screenBeneficialOwner(
                ubo.getFullName(), ubo.getDateOfBirth()))
            .collect(Collectors.toList());
        
        // Compare with previous results
        ScreeningResult previousResult = getLatestScreeningResult(merchant.getMerchantId());
        
        // Create new screening record
        MerchantScreeningResult screeningRecord = new MerchantScreeningResult();
        screeningRecord.setMerchantId(merchant.getMerchantId());
        screeningRecord.setScreeningType("PERIODIC");
        screeningRecord.setScreeningStatus(currentResult.getStatus());
        screeningRecord.setMatchDetails(currentResult.toJson());
        screeningRecord.setScreenedAt(LocalDateTime.now());
        
        merchantScreeningRepository.save(screeningRecord);
        
        return new RescreeningResult(previousResult, currentResult, uboResults);
    }
    
    private void createMonitoringAlert(Merchant merchant, RescreeningResult result) {
        MonitoringAlert alert = new MonitoringAlert();
        alert.setMerchantId(merchant.getMerchantId());
        alert.setAlertType("NEW_SANCTIONS_MATCH");
        alert.setAlertSeverity("CRITICAL");
        alert.setAlertDetails(result.toJson());
        alert.setCreatedAt(LocalDateTime.now());
        
        alertRepository.save(alert);
        
        // Send notification to compliance team
        notificationService.sendAlert(alert);
    }
}
```

---

## Compliance & Security Considerations

### 1. Auditability

#### Immutable Audit Trail

```java
@Entity
@Table(name = "audit_trail")
public class AuditTrail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;
    
    private Long merchantId;
    private String action;  // ONBOARDED, SCREENED, APPROVED, REJECTED, UPDATED
    private String performedBy;  // User ID or system
    private LocalDateTime performedAt;
    
    @Column(columnDefinition = "jsonb")
    private String evidence;  // Screening results, risk scores, etc.
    
    private String rulesVersion;  // e.g., "v3.2"
    private String decision;
    private String decisionReason;
    
    // Immutable - no setters after creation
}

@Service
public class AuditService {
    
    @Autowired
    private AuditTrailRepository auditRepository;
    
    public void logMerchantDecision(Long merchantId, OnboardingDecision decision, 
                                     RiskScore riskScore, String userId) {
        AuditTrail audit = new AuditTrail();
        audit.setMerchantId(merchantId);
        audit.setAction("ONBOARDING_DECISION");
        audit.setPerformedBy(userId);
        audit.setPerformedAt(LocalDateTime.now());
        audit.setEvidence(riskScore.toJson());
        audit.setRulesVersion(getRulesVersion());
        audit.setDecision(decision.name());
        audit.setDecisionReason(String.join("; ", riskScore.getReasons()));
        
        auditRepository.save(audit);
    }
    
    private String getRulesVersion() {
        // Version from git tag or config
        return configService.getConfig("rules.version", "v1.0");
    }
}
```

#### Versioned Risk Rules

```sql
CREATE TABLE risk_rules_versions (
    version_id SERIAL PRIMARY KEY,
    version TEXT NOT NULL,  -- e.g., "v3.2"
    rules_yaml TEXT NOT NULL,  -- Full rules YAML
    activated_at TIMESTAMP NOT NULL,
    activated_by TEXT NOT NULL,
    deactivated_at TIMESTAMP,
    is_active BOOLEAN DEFAULT true
);
```

### 2. PII & Security

#### Database Encryption

```properties
# application.properties

# Database encryption at rest (PostgreSQL)
spring.datasource.url=jdbc:postgresql://localhost:5432/aml_fraud_db?ssl=true&sslmode=require

# Column-level encryption for sensitive data
security.encryption.key=${ENCRYPTION_KEY}
security.encryption.algorithm=AES-256-GCM
```

#### Encrypted Sensitive Columns

```java
@Entity
public class BeneficialOwner {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long ownerId;
    
    private String fullName;
    
    @Convert(converter = EncryptedStringConverter.class)
    private String dateOfBirth;  // Encrypted
    
    @Convert(converter = EncryptedStringConverter.class)
    private String passportNumber;  // Encrypted
    
    @Convert(converter = EncryptedStringConverter.class)
    private String nationalId;  // Encrypted
    
    // ...
}

@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }
    
    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
```

#### Role-Based Access Control (RBAC)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/api/v1/cases/**").hasAnyRole("COMPLIANCE_OFFICER", "ADMIN")
                .antMatchers("/api/v1/merchants/*/pii").hasRole("COMPLIANCE_OFFICER")
                .antMatchers("/api/v1/screening/**").hasAnyRole("ANALYST", "COMPLIANCE_OFFICER")
                .antMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .and()
            .httpBasic();
    }
}

// User roles
public enum UserRole {
    ADMIN,              // Full system access
    COMPLIANCE_OFFICER, // View cases, make decisions, see full PII
    ANALYST,            // View cases, limited PII access
    AUDITOR             // Read-only access to audit trail
}
```

#### PII Masking in Logs

```java
@Component
public class LoggingAspect {
    
    @Around("execution(* com.posgateway.aml..*(..))")
    public Object maskPII(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        
        // Mask sensitive data in logs
        Object[] maskedArgs = Arrays.stream(args)
            .map(this::maskSensitiveData)
            .toArray();
        
        log.info("Method: {}, Args: {}", joinPoint.getSignature(), maskedArgs);
        
        return joinPoint.proceed();
    }
    
    private Object maskSensitiveData(Object arg) {
        if (arg instanceof String) {
            String str = (String) arg;
            // Mask credit card, SSN, passport patterns
            str = str.replaceAll("\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}", "****-****-****-****");
            str = str.replaceAll("\\d{3}-\\d{2}-\\d{4}", "***-**-****");
            return str;
        }
        return arg;
    }
}
```

### 3. Performance & Resilience

#### Circuit Breakers (Resilience4j)

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot2</artifactId>
    <version>2.1.0</version>
</dependency>
```

```java
@Service
public class ExternalAmlService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    @CircuitBreaker(name = "amlProvider", fallbackMethod = "fallbackScreening")
    @RateLimiter(name = "amlProvider")
    @Retry(name = "amlProvider")
    @Timeout(name = "amlProvider")
    public ScreeningResult screenWithProvider(String name) {
        // Call external AML provider
        return restTemplate.postForObject(
            amlProviderUrl + "/screen",
            new ScreeningRequest(name),
            ScreeningResult.class
        );
    }
    
    private ScreeningResult fallbackScreening(String name, Exception e) {
        log.warn("AML provider unavailable, using fallback: {}", e.getMessage());
        
        // Fallback to local screening only
        return sanctionsScreeningService.screenName(name, EntityType.PERSON);
    }
}
```

```yaml
# application.yml
resilience4j:
  circuitbreaker:
    instances:
      amlProvider:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
        sliding-window-size: 10
        minimum-number-of-calls: 5
  
  retry:
    instances:
      amlProvider:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
  
  timelimiter:
    instances:
      amlProvider:
        timeout-duration: 5s
  
  ratelimiter:
    instances:
      amlProvider:
        limit-for-period: 100
        limit-refresh-period: 1s
```

#### Async Screening with Callbacks

```java
@Service
public class AsyncMerchantOnboardingService {
    
    @Autowired
    private AsyncScreeningQueue screeningQueue;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Accept merchant application, enqueue screening
     */
    @Transactional
    public MerchantOnboardingResponse submitApplication(MerchantApplication application) {
        // 1. Save application (pending status)
        Merchant merchant = new Merchant();
        merchant.setStatus(MerchantStatus.PENDING_SCREENING);
        merchant.setApplicationData(application.toJson());
        merchant = merchantRepository.save(merchant);
        
        // 2. Enqueue screening job
        ScreeningJob job = new ScreeningJob(merchant.getMerchantId(), application);
        screeningQueue.enqueue(job);
        
        // 3. Return acknowledgment
        return new MerchantOnboardingResponse(
            merchant.getMerchantId(),
            "APPLICATION_RECEIVED",
            "Screening in progress. You will be notified when complete."
        );
    }
    
    /**
     * Background worker processes screening jobs
     */
    @Async
    @Scheduled(fixedDelay = 5000)  // Every 5 seconds
    public void processScreeningQueue() {
        ScreeningJob job = screeningQueue.dequeue();
        
        if (job != null) {
            try {
                // Perform screening
                ScreeningResult result = screeningService.screenMerchant(
                    job.getApplication().getLegalName(),
                    job.getApplication().getTradingName()
                );
                
                // Update merchant status
                updateMerchantStatus(job.getMerchantId(), result);
                
                // Send callback/notification
                notificationService.notifyMerchantScreeningComplete(
                    job.getMerchantId(), result
                );
                
            } catch (Exception e) {
                log.error("Screening job failed: {}", job.getMerchantId(), e);
                handleScreeningFailure(job);
            }
        }
    }
}
```

---

## REST DTOs and Response Models

### Request DTOs

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOnboardingRequest {
    
    @NotBlank
    private String legalName;
    
    private String tradingName;
    
    @NotBlank
    private String country;
    
    @NotBlank
    private String registrationNumber;
    
    private String taxId;
    
    @NotBlank
    private String mcc;  // Merchant Category Code
    
    @NotNull
    private Long expectedMonthlyVolume;  // In cents
    
    private String website;
    
    @Valid
    private Address businessAddress;
    
    @NotEmpty
    @Valid
    private List<BeneficialOwnerRequest> beneficialOwners;
    
    @Data
    public static class Address {
        private String street;
        private String city;
        private String state;
        private String postalCode;
        private String country;
    }
}

@Data
public class BeneficialOwnerRequest {
    
    @NotBlank
    private String fullName;
    
    @NotNull
    @Past
    private LocalDate dateOfBirth;
    
    @NotBlank
    private String nationality;
    
    private String countryOfResidence;
    
    @NotBlank
    private String passportNumber;
    
    @Min(25)
    @Max(100)
    private Integer ownershipPercentage;
}

@Data
public class ScreeningRequest {
    @NotBlank
    private String name;
    
    private LocalDate dateOfBirth;
    
    private String entityType;  // PERSON, ORGANIZATION
}
```

### Response DTOs

```java
@Data
@Builder
public class MerchantOnboardingResponse {
    private Long merchantId;
    private String applicationStatus;  // RECEIVED, APPROVED, REJECTED, REVIEW_REQUIRED
    private String decision;  // APPROVE, REJECT, ENHANCED_DUE_DILIGENCE, REVIEW
    private RiskScoreDTO riskScore;
    private ScreeningResultDTO merchantScreening;
    private List<ScreeningResultDTO> uboScreenings;
    private ComplianceCaseDTO complianceCase;  // If created
    private String message;
    private LocalDateTime processedAt;
}

@Data
@Builder
public class ScreeningResultDTO {
    private String name;
    private String screeningStatus;  // CLEAR, MATCH, POTENTIAL_MATCH
    private List<MatchDTO> matches;
    private LocalDateTime screenedAt;
    
    @Data
    @Builder
    public static class MatchDTO {
        private String matchedName;
        private Double similarityScore;
        private String listName;  // OFAC_SDN, UN_SC, EU_FSF
        private String entityType;
        private String matchType;  // NAME_MATCH, ALIAS_MATCH, DOB_CONFIRMED
        private Map<String, Object> details;
    }
}

@Data
@Builder
public class RiskScoreDTO {
    private Integer totalPoints;
    private String riskLevel;  // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> riskFactors;
    private String recommendedAction;
    private Boolean requiresEDD;
}

@Data
@Builder
public class ComplianceCaseDTO {
    private Long caseId;
    private String caseType;
    private String caseStatus;
    private String priority;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime dueDate;
}
```

---

## Compliance UI Dashboard

### Technology Stack

**Option 1: React (Recommended)**
- Modern, responsive UI
- Rich ecosystem
- Excellent for complex dashboards

**Option 2: Thymeleaf**
- Server-side rendering
- Simpler for Spring Boot
- Good for internal tools

### React Dashboard Components

```typescript
// MerchantCasesDashboard.tsx
interface ComplianceCase {
    caseId: number;
    merchantId: number;
    merchantName: string;
    caseType: string;
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
    status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'ESCALATED';
    riskScore: number;
    assignedTo: string;
    createdAt: string;
    dueDate: string;
}

export const MerchantCasesDashboard: React.FC = () => {
    const [cases, setCases] = useState<ComplianceCase[]>([]);
    const [filter, setFilter] = useState({ status: 'OPEN', priority: 'ALL' });
    
    useEffect(() => {
        fetchCases();
    }, [filter]);
    
    const fetchCases = async () => {
        const response = await fetch('/api/v1/cases?status=' + filter.status);
        const data = await response.json();
        setCases(data);
    };
    
    return (
        <div className="dashboard">
            <h1>Compliance Cases Dashboard</h1>
            
            {/* Filters */}
            <CaseFilters filter={filter} onFilterChange={setFilter} />
            
            {/* Summary Cards */}
            <div className="summary-cards">
                <SummaryCard title="Open Cases" count={cases.filter(c => c.status === 'OPEN').length} />
                <SummaryCard title="High Priority" count={cases.filter(c => c.priority === 'HIGH').length} color="red" />
                <SummaryCard title="Overdue" count={cases.filter(c => isOverdue(c)).length} color="orange" />
            </div>
            
            {/* Cases Table */}
            <CasesTable cases={cases} onCaseClick={handleCaseClick} />
        </div>
    );
};

// CaseDetailView.tsx
export const CaseDetailView: React.FC<{ caseId: number }> = ({ caseId }) => {
    const [caseDetail, setCaseDetail] = useState<CaseDetail | null>(null);
    
    const handleApprove = async () => {
        await fetch(`/api/v1/cases/${caseId}/resolve`, {
            method: 'PUT',
            body: JSON.stringify({ decision: 'APPROVE', notes: '...' })
        });
    };
    
    const handleReject = async () => {
        // Similar to approve
    };
    
    return (
        <div className="case-detail">
            <h2>Case #{caseId}</h2>
            
            {/* Merchant Information */}
            <Section title="Merchant Information">
                <InfoField label="Legal Name" value={caseDetail.merchant.legalName} />
                <InfoField label="Country" value={caseDetail.merchant.country} />
                <InfoField label="MCC" value={caseDetail.merchant.mcc} />
            </Section>
            
            {/* Screening Results */}
            <Section title="Screening Results">
                <ScreeningMatches matches={caseDetail.screening.matches} />
            </Section>
            
            {/* Risk Score */}
            <Section title="Risk Assessment">
                <RiskScoreDisplay score={caseDetail.riskScore} />
            </Section>
            
            {/* Decision Actions */}
            <div className="actions">
                <Button onClick={handleApprove} variant="success">Approve</Button>
                <Button onClick={handleReject} variant="danger">Reject</Button>
                <Button variant="secondary">Request More Info</Button>
            </div>
        </div>
    );
};
```

### Spring Boot REST Controllers

```java
@RestController
@RequestMapping("/api/v1/cases")
public class ComplianceCaseController {
    
    @Autowired
    private ComplianceCaseService caseService;
    
    @GetMapping
    public ResponseEntity<List<ComplianceCaseDTO>> getCases(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String assignedTo) {
        
        List<ComplianceCase> cases = caseService.findCases(status, priority, assignedTo);
        return ResponseEntity.ok(cases.stream()
            .map(this::toDTO)
            .collect(Collectors.toList()));
    }
    
    @GetMapping("/{caseId}")
    public ResponseEntity<CaseDetailDTO> getCaseDetail(@PathVariable Long caseId) {
        CaseDetail detail = caseService.getCaseDetail(caseId);
        return ResponseEntity.ok(toDetailDTO(detail));
    }
    
    @PutMapping("/{caseId}/resolve")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Void> resolveCase(
            @PathVariable Long caseId,
            @RequestBody CaseResolutionRequest request,
            @AuthenticationPrincipal UserDetails user) {
        
        caseService.resolveCase(caseId, request.getDecision(), 
                                request.getNotes(), user.getUsername());
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{caseId}/assign")
    @PreAuthorize("hasRole('COMPLIANCE_OFFICER')")
    public ResponseEntity<Void> assignCase(
            @PathVariable Long caseId,
            @RequestParam String assignTo) {
        
        caseService.assignCase(caseId, assignTo);
        return ResponseEntity.ok().build();
    }
}
```

---

## Rules DSL for Risk Engine

### JSON-Based Rules Configuration

This allows product/compliance teams to modify rules without code changes.

```json
{
  "rulesVersion": "v3.2",
  "effectiveDate": "2025-01-01",
  "rules": [
    {
      "ruleId": "R001",
      "name": "High Risk Country",
      "enabled": true,
      "priority": 1,
      "condition": {
        "field": "merchant.country",
        "operator": "IN",
        "values": ["AF", "IR", "KP", "SY", "YE"]
      },
      "actions": [
        {
          "type": "ADD_POINTS",
          "value": 50
        },
        {
          "type": "ADD_REASON",
          "value": "Merchant operates in high-risk country: {merchant.country}"
        }
      ]
    },
    {
      "ruleId": "R002",
      "name": "High Transaction Volume",
      "enabled": true,
      "priority": 3,
      "condition": {
        "field": "merchant.expectedMonthlyVolume",
        "operator": "GREATER_THAN",
        "value": 1000000
      },
      "actions": [
        {
          "type": "ADD_POINTS",
          "value": 20
        },
        {
          "type": "REQUIRE_EDD",
          "value": true
        },
        {
          "type": "ADD_REASON",
          "value": "High monthly volume: ${merchant.expectedMonthlyVolume}"
        }
      ]
    },
    {
      "ruleId": "R003",
      "name": "Sanctions Match - Auto Reject",
      "enabled": true,
      "priority": 10,
      "condition": {
        "field": "screeningResult.status",
        "operator": "EQUALS",
        "value": "MATCH"
      },
      "actions": [
        {
          "type": "SET_DECISION",
          "value": "REJECT"
        },
        {
          "type": "ADD_POINTS",
          "value": 100
        }
      ]
    }
  ],
  "thresholds": {
    "autoApprove": 30,
    "manualReview": 50,
    "enhancedDueDiligence": 50,
    "autoReject": 80
  }
}
```

### Rules Management UI

```typescript
// RulesEditor.tsx
export const RulesEditor: React.FC = () => {
    const [rules, setRules] = useState<Rule[]>([]);
    
    const handleUpdateRule = async (ruleId: string, updates: Partial<Rule>) => {
        await fetch(`/api/v1/admin/rules/${ruleId}`, {
            method: 'PUT',
            body: JSON.stringify(updates)
        });
        fetchRules();
    };
    
    const handleToggleRule = (ruleId: string, enabled: boolean) => {
        handleUpdateRule(ruleId, { enabled });
    };
    
    return (
        <div className="rules-editor">
            <h2>Risk Rules Configuration</h2>
            
            <table>
                <thead>
                    <tr>
                        <th>Rule ID</th>
                        <th>Name</th>
                        <th>Priority</th>
                        <th>Points</th>
                        <th>Enabled</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    {rules.map(rule => (
                        <tr key={rule.ruleId}>
                            <td>{rule.ruleId}</td>
                            <td>{rule.name}</td>
                            <td>{rule.priority}</td>
                            <td>{rule.points}</td>
                            <td>
                                <Switch 
                                    checked={rule.enabled}
                                    onChange={(e) => handleToggleRule(rule.ruleId, e.target.checked)}
                                />
                            </td>
                            <td>
                                <Button onClick={() => editRule(rule)}>Edit</Button>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
            
            <ThresholdsEditor thresholds={thresholds} onUpdate={updateThresholds} />
        </div>
    );
};
```

### Rules Management API

```java
@RestController
@RequestMapping("/api/v1/admin/rules")
@PreAuthorize("hasRole('ADMIN')")
public class RulesManagementController {
    
    @Autowired
    private RulesConfigService rulesConfigService;
    
    @GetMapping
    public ResponseEntity<RulesConfiguration> getRules() {
        return ResponseEntity.ok(rulesConfigService.getCurrentRules());
    }
    
    @PutMapping("/{ruleId}")
    public ResponseEntity<Void> updateRule(
            @PathVariable String ruleId,
            @RequestBody RuleUpdateRequest request,
            @AuthenticationPrincipal UserDetails user) {
        
        rulesConfigService.updateRule(ruleId, request, user.getUsername());
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/version")
    public ResponseEntity<String> createNewVersion(
            @RequestBody RulesConfiguration newRules,
            @AuthenticationPrincipal UserDetails user) {
        
        String newVersion = rulesConfigService.createVersion(newRules, user.getUsername());
        return ResponseEntity.ok(newVersion);
    }
    
    @PutMapping("/thresholds")
    public ResponseEntity<Void> updateThresholds(
            @RequestBody ThresholdsUpdateRequest request) {
        
        rulesConfigService.updateThresholds(request);
        return ResponseEntity.ok().build();
    }
}
```

---

## Deployment Checklist

- [ ] Create database tables
- [ ] Add Maven dependencies
- [ ] Configure application properties
- [ ] Run initial sanctions list download
- [ ] Verify downloaded data in database
- [ ] Test screening with known sanctions names
- [ ] Set up daily download cron job
- [ ] Configure monitoring and alerts
- [ ] Load test screening service
- [ ] Deploy to production

---

## Advantages of Roll-Your-Own Approach

### vs. External AML Service Providers

**Pros:**
- ✅ **Cost savings:** Free sanctions data (vs $100-$1,000+/month)
- ✅ **No API rate limits:** Unlimited screening
- ✅ **Full control:** Customize matching logic and thresholds
- ✅ **Data ownership:** Complete audit trail in your database
- ✅ **Faster:** No external API latency
- ✅ **Offline capability:** Works without internet

**Cons:**
- ❌ **Maintenance:** Must maintain download/update logic
- ❌ **No adverse media:** Only sanctions/PEP (not negative news)
- ❌ **Limited PEP coverage:** OpenSanctions has fewer PEPs than commercial providers
- ❌ **Development time:** Requires initial implementation

### Hybrid Approach (Recommended)

**Best of both worlds:**
1. **Roll-your-own for sanctions screening** (free, fast, unlimited)
2. **External API for adverse media** (ComplyAdvantage/Sumsub for news)
3. **External API for enhanced PEP** (if needed for comprehensive coverage)

---

**Last Updated:** 2025-12-05  
**Status:** Implementation Ready

---

## Mitigation Strategies for "Roll-Your-Own" Cons

To address the common downsides of an in-house implementation, we adopt the following strategies:

### 1. Maintenance & liability (The "Download Logic" Burden)
**Con:** You must maintain the download/update logic and ensure it never fails.
**Mitigation:** **Self-Healing & Alerting Architecture**
- **Automated Retries:** The download service implements exponential backoff retries for network glitches.
- **Stale Data Watchdog:** A separate scheduled task checks the `last_updated` timestamp of the sanctions list. If data > 24 hours old, it triggers a **CRITICAL** alert to the DevOps team (Slack/PagerDuty).
- **Fallback Source:** If OpenSanctions is down, the system automatically falls back to the direct OFAC/UN XML feeds (parsing logic implemented as backup).

### 2. Limited Data Coverage (No Adverse Media)
**Con:** OpenSanctions/OFAC covers Sanctions & PEPs well, but lacks broader "Adverse Media" (negative news).
**Mitigation:** **Hybrid "Traffic Light" Model**
We use a cost-effective hybrid approach:
- **Green (Clean):** 95% of users pass our internal Sanctions/PEP check. **Cost: $0**.
- **Red (Match):** Validated internal connection to a blocked entity. Auto-reject. **Cost: $0**.
- **Yellow (Risk/Unclear):** User has no sanctions match but triggers a specific risk rule (e.g., High-Risk Country + High Volume).
    - **Action:** Triggers an **On-Demand API Call** to a premium provider (e.g., Sumsub/ComplyAdvantage) *only* for this user to check Adverse Media.
    - **Result:** You pay for <5% of your volume, getting full coverage where it matters most while keeping 95% of checks free.

### 3. Limited PEP Coverage
**Con:** Open-source PEP lists are less comprehensive than commercial databases (e.g., missing family members).
**Mitigation:** **Risk-Based PEP Screening**
- **Standard Due Diligence (SDD):** For low-risk merchants (e.g., local bakery), open-source PEP checks are sufficient.
- **Enhanced Due Diligence (EDD):** For high-risk entities (e.g., Casinos, Crypto), force the **Manual Review** workflow which includes a mandatory check against a commercial database (via on-demand lookup) or manual simplified search.

### 4. Development Time
**Con:** Requires initial coding effort.
**Mitigation:** **Modular Microservice**
- We implement this as a standalone `Sanctions-Service` with a clear API.
- Once built, it requires little feature work.
- The "Rule Engine" approach decouples logic from code, allowing compliance officers to tweak thresholds without developer intervention.
