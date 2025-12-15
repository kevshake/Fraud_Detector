package com.posgateway.aml.service.aml;

import com.aerospike.client.*;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.RecordSet;
import com.aerospike.client.query.Statement;
import com.posgateway.aml.model.ScreeningResult;
import com.posgateway.aml.model.ScreeningResult.EntityType;
import com.posgateway.aml.model.ScreeningResult.Match;
import com.posgateway.aml.model.ScreeningResult.MatchType;
import com.posgateway.aml.model.ScreeningResult.ScreeningStatus;
import com.posgateway.aml.service.sanctions.NameMatchingService;
import com.posgateway.aml.service.AerospikeConnectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aerospike-based sanctions screening service (Tier 2)
 * Used for existing merchants or when Sumsub is unavailable
 */
@Service
@Slf4j
public class AerospikeSanctionsScreeningService {

    @Autowired
    private AerospikeConnectionService aerospikeService;

    @Autowired
    private NameMatchingService nameMatchingService;

    @Value("${aerospike.namespace:sanctions}")
    private String namespace;

    @Value("${sanctions.matching.similarity.threshold:0.8}")
    private double similarityThreshold;

    @Value("${sanctions.cache.enabled:true}")
    private boolean cacheEnabled;

    @Value("${sanctions.cache.ttl.hours:24}")
    private int cacheTtlHours;

    /**
     * Screen a name against Aerospike sanctions database
     */
    @org.springframework.cache.annotation.Cacheable(value = "screeningResults", key = "#name")
    public ScreeningResult screenName(String name, EntityType entityType) {
        log.info("Screening name '{}' against Aerospike sanctions (type: {})", name, entityType);

        // Check cache first
        if (cacheEnabled) {
            ScreeningResult cachedResult = getCachedScreeningResult(name);
            if (cachedResult != null) {
                log.debug("Cache hit for name '{}'", name);
                return cachedResult;
            }
        }

        try {
            // Generate phonetic codes for fast lookup
            String phoneticCode = nameMatchingService.generatePhoneticCode(name);
            String alternateCode = nameMatchingService.generateAlternatePhoneticCode(name);

            log.debug("Phonetic codes: primary='{}', alternate='{}'", phoneticCode, alternateCode);

            // Query Aerospike by phonetic code (fast indexed lookup)
            List<Match> matches = new ArrayList<>();
            matches.addAll(findMatchesByPhoneticCode(phoneticCode, name, entityType));

            // Also check alternate phonetic code if different
            if (!phoneticCode.equals(alternateCode)) {
                matches.addAll(findMatchesByPhoneticCode(alternateCode, name, entityType));
            }

            // Build result
            ScreeningResult result = ScreeningResult.builder()
                    .screenedName(name)
                    .entityType(entityType)
                    .status(determineStatus(matches))
                    .matchCount(matches.size())
                    .highestMatchScore(getHighestScore(matches))
                    .matches(matches)
                    .screenedAt(LocalDateTime.now())
                    .screeningProvider("AEROSPIKE")
                    .build();

            // Cache result
            if (cacheEnabled && result.getStatus() == ScreeningStatus.CLEAR) {
                cacheScreeningResult(name, result);
            }

            log.info("Screening complete: {} matches found (status: {})", matches.size(), result.getStatus());

            return result;

        } catch (Exception e) {
            log.error("Error screening name '{}': {}", name, e.getMessage(), e);
            throw new RuntimeException("Aerospike screening failed", e);
        }
    }

    /**
     * Screen a merchant (legal name + trading name)
     */
    public ScreeningResult screenMerchant(String legalName, String tradingName) {
        log.info("Screening merchant: legalName='{}', tradingName='{}'", legalName, tradingName);

        // Screen legal name
        ScreeningResult legalNameResult = screenName(legalName, EntityType.ORGANIZATION);

        // Screen trading name if different
        if (tradingName != null && !tradingName.equals(legalName)) {
            ScreeningResult tradingNameResult = screenName(tradingName, EntityType.ORGANIZATION);

            // Merge results
            if (tradingNameResult.hasMatches()) {
                legalNameResult.getMatches().addAll(tradingNameResult.getMatches());
                legalNameResult.setStatus(determineStatus(legalNameResult.getMatches()));
                legalNameResult.setMatchCount(legalNameResult.getMatches().size());
                legalNameResult.setHighestMatchScore(getHighestScore(legalNameResult.getMatches()));
            }
        }

        return legalNameResult;
    }

    /**
     * Screen a beneficial owner
     */
    public ScreeningResult screenBeneficialOwner(String fullName, LocalDate dateOfBirth) {
        log.info("Screening beneficial owner: name='{}', dob='{}'", fullName, dateOfBirth);

        ScreeningResult result = screenName(fullName, EntityType.PERSON);

        // If DOB provided, verify matches
        if (dateOfBirth != null && result.hasMatches()) {
            result.getMatches().forEach(match -> {
                if (match.getDateOfBirth() != null && match.getDateOfBirth().equals(dateOfBirth)) {
                    match.setMatchType(MatchType.DOB_CONFIRMED);
                    log.warn("DOB match confirmed for '{}': {}", fullName, dateOfBirth);
                }
            });
        }

        return result;
    }

    /**
     * Find matches in Aerospike by phonetic code
     */
    private List<Match> findMatchesByPhoneticCode(String phoneticCode, String searchName, EntityType entityType) {
        List<Match> matches = new ArrayList<>();

        if (phoneticCode == null || phoneticCode.isEmpty()) {
            return matches;
        }

        AerospikeClient client = aerospikeService.getClient();
        if (client == null) {
            log.error("Aerospike client not available");
            return matches;
        }

        try {
            // Query using secondary index on name_metaphone
            Statement stmt = new Statement();
            stmt.setNamespace(namespace);
            stmt.setSetName("entities");
            stmt.setFilter(Filter.equal("name_metaphone", phoneticCode));

            QueryPolicy policy = new QueryPolicy();
            policy.maxConcurrentNodes = 1;
            policy.recordQueueSize = 50;

            RecordSet recordSet = client.query(policy, stmt);

            try {
                while (recordSet.next()) {
                    com.aerospike.client.Record record = recordSet.getRecord();

                    // Extract data from Aerospike bins
                    String fullName = (String) record.bins.get("full_name");
                    String entityTypeStr = (String) record.bins.get("entity_type");

                    // Filter by entity type if specified
                    if (entityType != null && !entityType.name().equals(entityTypeStr)) {
                        continue;
                    }

                    // Calculate similarity
                    double similarityScore = nameMatchingService.calculateSimilarityScore(searchName, fullName);

                    // Only add if similarity is above threshold
                    if (similarityScore >= similarityThreshold) {
                        Match match = buildMatchFromRecord(record, similarityScore);
                        matches.add(match);

                        log.debug("Match found: '{}' <-> '{}' (score: {:.2f})",
                                searchName, fullName, similarityScore);
                    }
                }
            } finally {
                recordSet.close();
            }

        } catch (AerospikeException e) {
            log.error("Aerospike query error: {}", e.getMessage(), e);
        }

        return matches;
    }

    /**
     * Build Match object from Aerospike record
     */
    private Match buildMatchFromRecord(com.aerospike.client.Record record, double similarityScore) {
        return Match.builder()
                .matchedName((String) record.bins.get("full_name"))
                .aliases((List<String>) record.bins.get("aliases"))
                .similarityScore(similarityScore)
                .listName((String) record.bins.get("list_name"))
                .entityType(EntityType.valueOf((String) record.bins.get("entity_type")))
                .matchType(MatchType.PHONETIC_MATCH)
                .dateOfBirth(parseDateOfBirth(record.bins.get("date_of_birth")))
                .nationality((List<String>) record.bins.get("nationality"))
                .sanctionType((String) record.bins.get("sanction_type"))
                .programs((List<String>) record.bins.get("program"))
                .build();
    }

    /**
     * Get cached screening result
     */
    private ScreeningResult getCachedScreeningResult(String name) {
        try {
            AerospikeClient client = aerospikeService.getClient();
            if (client == null)
                return null;

            String cacheKey = "CACHE:" + name.hashCode();
            Key key = new Key(namespace, "screening_cache", cacheKey);

            com.aerospike.client.Record record = client.get(null, key);
            if (record != null) {
                // Parse cached result (implement deserialization)
                return parseCachedResult(record);
            }
        } catch (Exception e) {
            log.warn("Cache lookup failed: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Cache screening result in Aerospike
     */
    private void cacheScreeningResult(String name, ScreeningResult result) {
        try {
            AerospikeClient client = aerospikeService.getClient();
            if (client == null)
                return;

            String cacheKey = "CACHE:" + name.hashCode();
            Key key = new Key(namespace, "screening_cache", cacheKey);

            // Create bins for cache
            Bin nameBin = new Bin("name", name);
            Bin statusBin = new Bin("status", result.getStatus().name());
            Bin screenedAtBin = new Bin("screened_at", System.currentTimeMillis());

            // Write with TTL
            com.aerospike.client.policy.WritePolicy policy = new com.aerospike.client.policy.WritePolicy();
            policy.expiration = cacheTtlHours * 3600; // Convert hours to seconds

            client.put(policy, key, nameBin, statusBin, screenedAtBin);

            log.debug("Cached screening result for '{}'", name);

        } catch (Exception e) {
            log.warn("Failed to cache result: {}", e.getMessage());
        }
    }

    private ScreeningStatus determineStatus(List<Match> matches) {
        if (matches.isEmpty()) {
            return ScreeningStatus.CLEAR;
        }

        // Check if any match has high confidence (>0.95)
        boolean highConfidenceMatch = matches.stream()
                .anyMatch(m -> m.getSimilarityScore() >= 0.95);

        return highConfidenceMatch ? ScreeningStatus.MATCH : ScreeningStatus.POTENTIAL_MATCH;
    }

    private Double getHighestScore(List<Match> matches) {
        return matches.stream()
                .map(Match::getSimilarityScore)
                .max(Double::compareTo)
                .orElse(0.0);
    }

    private LocalDate parseDateOfBirth(Object dob) {
        if (dob instanceof Long) {
            long epochSeconds = (Long) dob;
            return LocalDate.ofEpochDay(epochSeconds / 86400);
        }
        return null;
    }

    private ScreeningResult parseCachedResult(com.aerospike.client.Record record) {
        // Implement cache deserialization
        return ScreeningResult.builder()
                .screenedName((String) record.bins.get("name"))
                .status(ScreeningStatus.valueOf((String) record.bins.get("status")))
                .screenedAt(LocalDateTime.now())
                .screeningProvider("AEROSPIKE_CACHE")
                .matches(new ArrayList<>())
                .build();
    }
}
