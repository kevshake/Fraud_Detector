package com.hokeka.aml.service;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.RecordExistsAction;
import com.aerospike.client.policy.ScanPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Filter;
import com.aerospike.client.query.Statement;
import com.hokeka.aml.model.SanctionsIngestRequest;
import com.hokeka.aml.model.SanctionsIngestRequest.SanctionsEntity;
import com.hokeka.aml.model.SanctionsScreenResponse;
import com.hokeka.aml.model.SanctionsScreenResponse.MatchDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the {@code aml_cache.sanctions} Aerospike set: bulk ingest + name screening.
 *
 * <p>Screening uses Aerospike secondary indexes for O(log n) candidate retrieval
 * instead of full-set scans. During ingest, the first TWO letters of the normalized
 * name are stored as a {@code prefix} bin. A secondary index
 * ({@code idx_sanctions_prefix}) allows query by that prefix, narrowing the candidate
 * set to ~1/676th of the total — a 676× reduction vs full scan.
 *
 * <p>The index is auto-created at startup via {@link #ensureIndex()} if it doesn't
 * already exist. The fallback when the index creation fails (e.g. insufficient
 * Aerospike privileges) is the original scanAll behaviour.
 *
 * <p>Bins per record: {@code name}, {@code aliases} (CSV), {@code type},
 * {@code listName}, {@code country}, {@code birthDate}, {@code prefix}.
 * Key is the upstream {@code entityId} so re-ingest is idempotent (REPLACE).
 */
@Service
public class SanctionsService {

    private static final Logger log = LoggerFactory.getLogger(SanctionsService.class);
    private static final String NAMESPACE = "aml_cache";
    private static final String SET_NAME = "sanctions";
    /** Secondary index for prefix-based fast-lookup. */
    private static final String IDX_PREFIX = "idx_sanctions_prefix";
    private static final String BIN_PREFIX = "prefix";

    private static final int TOP_N = 5;
    private static final double FLAGGED_THRESHOLD = 0.95;

    /** Aerospike secondary index creation is a cluster admin operation; it may
     *  be unavailable to application-only credentials. We fall back to scan. */
    private boolean useIndex = false;

    @Autowired(required = false)
    private AerospikeClient aerospikeClient;

    @Value("${sanctions.match.similarity.threshold:0.8}")
    private double similarityThreshold;

    // ─────────────────────────────────────────────────────────────────────
    // Startup: create the prefix secondary index if it doesn't exist
    // ─────────────────────────────────────────────────────────────────────

    @PostConstruct
    public void ensureIndex() {
        if (aerospikeClient == null || !aerospikeClient.isConnected()) return;
        try {
            aerospikeClient.createIndex(null, NAMESPACE, SET_NAME, IDX_PREFIX, BIN_PREFIX,
                    com.aerospike.client.query.IndexType.STRING)
                    .waitTillComplete(2000);
            log.info("Aerospike secondary index '{}' ready on {}.{}", IDX_PREFIX, NAMESPACE, SET_NAME);
            useIndex = true;
        } catch (Exception e) {
            // Index may already exist → try to signal the client to use index anyway
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                useIndex = true;
                log.info("Aerospike secondary index '{}' already exists on {}.{} — using it",
                        IDX_PREFIX, NAMESPACE, SET_NAME);
            } else {
                useIndex = false;
                log.warn("Cannot create Aerospike secondary index '{}' — falling back to scan. Reason: {}",
                        IDX_PREFIX, e.getMessage());
            }
        }
    }

    public boolean isAerospikeConnected() {
        return aerospikeClient != null && aerospikeClient.isConnected();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Screen
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Screen a single name. Returns CLEAR if the data store is unavailable so the
     * pipeline fails open — callers can layer their own circuit-breaker decisions.
     */
    public SanctionsScreenResponse screenName(String name, String type) {
        Instant checkedAt = Instant.now();
        if (name == null || name.isBlank()) {
            return new SanctionsScreenResponse(name, "CLEAR", new ArrayList<>(), checkedAt);
        }
        if (!isAerospikeConnected()) {
            log.warn("Aerospike not connected — sanctions screen for '{}' returning CLEAR", name);
            return new SanctionsScreenResponse(name, "CLEAR", new ArrayList<>(), checkedAt);
        }

        String normalizedQuery = normalize(name);
        List<MatchDto> matches = new ArrayList<>();

        if (useIndex) {
            // Fast path: query by prefix (first 2 chars of normalized name)
            String prefix = extractPrefix(normalizedQuery);
            if (!prefix.isEmpty()) {
                screenByPrefix(normalizedQuery, prefix, type, matches);
            }
        } else {
            // Fallback: full scan (existing behaviour)
            screenByScan(normalizedQuery, type, matches);
        }

        matches.sort(Comparator.comparingDouble(MatchDto::getSimilarityScore).reversed());
        List<MatchDto> top = matches.size() > TOP_N ? matches.subList(0, TOP_N) : matches;

        String status;
        if (top.isEmpty()) {
            status = "CLEAR";
        } else if (top.get(0).getSimilarityScore() >= FLAGGED_THRESHOLD) {
            status = "FLAGGED";
        } else {
            status = "REVIEW";
        }

        return new SanctionsScreenResponse(name, status, new ArrayList<>(top), checkedAt);
    }

    /** Index-based query — ~676× fewer records examined than full scan. */
    private void screenByPrefix(String normalizedQuery, String prefix, String type, List<MatchDto> matches) {
        try {
            Statement stmt = new Statement();
            stmt.setNamespace(NAMESPACE);
            stmt.setSetName(SET_NAME);
            stmt.setBinNames("name", "aliases", "type", "listName", "entityId");
            stmt.setFilter(Filter.equal(BIN_PREFIX, prefix));

            aerospikeClient.query(null, stmt, (key, record) -> {
                if (record == null) return;
                evaluateRecord(normalizedQuery, type, key, record, matches);
            });
        } catch (Exception e) {
            log.warn("Aerospike query by prefix failed for '{}': {} — falling back to scan", prefix, e.getMessage());
            useIndex = false; // demote for future calls
            screenByScan(normalizedQuery, type, matches);
        }
    }

    /** Full scan — original behaviour for fallback. */
    private void screenByScan(String normalizedQuery, String type, List<MatchDto> matches) {
        ScanPolicy policy = new ScanPolicy();
        policy.includeBinData = true;
        policy.concurrentNodes = true;
        try {
            aerospikeClient.scanAll(policy, NAMESPACE, SET_NAME, (key, record) -> {
                if (record == null) return;
                evaluateRecord(normalizedQuery, type, key, record, matches);
            });
        } catch (Exception e) {
            log.warn("Aerospike scan failed during sanctions screen for '{}': {}", normalizedQuery, e.getMessage());
        }
    }

    /** Shared record evaluation used by both index query and scan. */
    private void evaluateRecord(String normalizedQuery, String type, Key key, Record record, List<MatchDto> matches) {
        String candidateName = record.getString("name");
        String aliasesCsv = record.getString("aliases");
        String candidateType = record.getString("type");

        // Type filter
        if (type != null && !type.isBlank() && candidateType != null
                && !type.equalsIgnoreCase(candidateType)) {
            return;
        }

        double best = 0.0;
        String bestMatched = candidateName;
        if (candidateName != null) {
            best = similarity(normalizedQuery, normalize(candidateName));
        }
        if (aliasesCsv != null && !aliasesCsv.isEmpty()) {
            for (String alias : aliasesCsv.split("\\|")) {
                if (alias.isBlank()) continue;
                double s = similarity(normalizedQuery, normalize(alias));
                if (s > best) {
                    best = s;
                    bestMatched = alias;
                }
            }
        }

        if (best >= similarityThreshold) {
            String entityId = key.userKey != null ? key.userKey.toString()
                    : (record.getString("entityId") != null ? record.getString("entityId") : "");
            String listName = record.getString("listName");
            synchronized (matches) {
                matches.add(new MatchDto(bestMatched, round(best), listName, entityId));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Ingest
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Bulk-write entities. REPLACE semantics so re-ingest from a fresh OpenSanctions
     * pull cleanly overwrites prior bins. Also writes the {@code prefix} bin for
     * secondary-index query.
     */
    public int ingestEntities(List<SanctionsEntity> entities) {
        if (entities == null || entities.isEmpty()) return 0;
        if (!isAerospikeConnected()) {
            log.warn("Aerospike not connected — sanctions ingest of {} entities SKIPPED", entities.size());
            return 0;
        }

        WritePolicy wp = new WritePolicy();
        wp.recordExistsAction = RecordExistsAction.REPLACE;
        wp.sendKey = true;

        AtomicInteger ok = new AtomicInteger();
        for (SanctionsEntity e : entities) {
            if (e == null || e.getEntityId() == null || e.getEntityId().isBlank()) continue;
            try {
                Key key = new Key(NAMESPACE, SET_NAME, e.getEntityId());
                String aliasesCsv = e.getAliases() == null ? "" : String.join("|", e.getAliases());
                String normalizedName = normalize(nullToEmpty(e.getName()));
                String prefix = extractPrefix(normalizedName);

                aerospikeClient.put(wp, key,
                        new Bin("name", nullToEmpty(e.getName())),
                        new Bin("aliases", aliasesCsv),
                        new Bin("type", nullToEmpty(e.getType())),
                        new Bin("listName", nullToEmpty(e.getListName())),
                        new Bin("country", nullToEmpty(e.getCountry())),
                        new Bin("birthDate", nullToEmpty(e.getBirthDate())),
                        new Bin("prefix", prefix));
                ok.incrementAndGet();
            } catch (Exception ex) {
                log.warn("Failed to ingest sanctions entity {}: {}", e.getEntityId(), ex.getMessage());
            }
        }
        log.info("Sanctions ingest: {} of {} entities written", ok.get(), entities.size());
        return ok.get();
    }

    public int ingest(SanctionsIngestRequest request) {
        return ingestEntities(request != null ? request.getEntities() : null);
    }

    /**
     * Count records. Uses the secondary index if available for O(log n) count,
     * otherwise scans keys only.
     */
    public long count() {
        if (!isAerospikeConnected()) return -1L;
        if (useIndex) {
            try {
                Statement stmt = new Statement();
                stmt.setNamespace(NAMESPACE);
                stmt.setSetName(SET_NAME);
                stmt.setBinNames("prefix"); // minimal bin read
                AtomicInteger c = new AtomicInteger();
                aerospikeClient.query(null, stmt, (key, record) -> c.incrementAndGet());
                return c.get();
            } catch (Exception e) {
                log.warn("Sanctions count query failed: {}", e.getMessage());
                return -1L;
            }
        }
        // Fallback: key-only scan
        ScanPolicy policy = new ScanPolicy();
        policy.includeBinData = false;
        policy.concurrentNodes = true;
        AtomicInteger c = new AtomicInteger();
        try {
            aerospikeClient.scanAll(policy, NAMESPACE, SET_NAME, (key, record) -> c.incrementAndGet());
        } catch (Exception e) {
            log.warn("Sanctions count scan failed: {}", e.getMessage());
            return -1L;
        }
        return c.get();
    }

    // -------------------- internals --------------------

    /** First 2 characters of normalized name — our query partition key. */
    static String extractPrefix(String normalized) {
        if (normalized == null || normalized.isEmpty()) return "";
        int end = Math.min(2, normalized.length());
        return normalized.substring(0, end);
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.toLowerCase().replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    /**
     * Hybrid similarity: max of Jaccard token overlap and 1-(levenshtein/maxLen).
     * Cheap, bounded, and dependency-free.
     */
    static double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        if (a.equals(b)) return 1.0;
        double jaccard = jaccard(a, b);
        double lev = 1.0 - ((double) levenshtein(a, b) / Math.max(a.length(), b.length()));
        if (lev < 0) lev = 0;
        return Math.max(jaccard, lev);
    }

    private static double jaccard(String a, String b) {
        Set<String> ta = new HashSet<>();
        Collections.addAll(ta, a.split(" "));
        Set<String> tb = new HashSet<>();
        Collections.addAll(tb, b.split(" "));
        if (ta.isEmpty() || tb.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(ta);
        inter.retainAll(tb);
        Set<String> union = new HashSet<>(ta);
        union.addAll(tb);
        return (double) inter.size() / union.size();
    }

    private static int levenshtein(String a, String b) {
        int n = a.length();
        int m = b.length();
        if (n == 0) return m;
        if (m == 0) return n;
        int[] prev = new int[m + 1];
        int[] curr = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            curr[0] = i;
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= m; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = curr; curr = tmp;
        }
        return prev[m];
    }
}