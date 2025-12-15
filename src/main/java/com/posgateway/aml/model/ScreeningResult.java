package com.posgateway.aml.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Screening result model for sanctions and PEP screening
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningResult {

    private String screenedName;
    private EntityType entityType;
    private ScreeningStatus status;
    private Integer matchCount;
    private Double highestMatchScore;

    @Builder.Default
    private List<Match> matches = new ArrayList<>();

    private LocalDateTime screenedAt;
    private String screeningProvider; // AEROSPIKE, SUMSUB, COMPLYADVANTAGE

    /**
     * Individual match within screening result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Match {
        private String matchedName;
        private List<String> aliases;
        private Double similarityScore;
        private String listName; // OFAC_SDN, UN_SC, EU_FSF, PEP
        private EntityType entityType;
        private MatchType matchType; // NAME_MATCH, ALIAS_MATCH, DOB_CONFIRMED

        // Additional details
        private LocalDate dateOfBirth;
        private List<String> nationality;
        private String sanctionType;
        private List<String> programs;
        private String pepLevel; // CURRENT, FORMER, RCA
        private String position;

        // Raw data for audit
        private Map<String, Object> rawData;
    }

    public enum ScreeningStatus {
        CLEAR,
        POTENTIAL_MATCH,
        MATCH
    }

    public enum EntityType {
        PERSON,
        ORGANIZATION,
        VESSEL,
        UNKNOWN
    }

    public enum MatchType {
        NAME_MATCH,
        ALIAS_MATCH,
        DOB_CONFIRMED,
        PHONETIC_MATCH
    }

    /**
     * Check if screening found any matches
     */
    public boolean hasMatches() {
        return matches != null && !matches.isEmpty();
    }

    /**
     * Get match count
     */
    public int getMatchCount() {
        return matches != null ? matches.size() : 0;
    }
}
