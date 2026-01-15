# AML Graph Analytics & ML Decision Engine - TODO Roadmap

> This roadmap integrates **Neo4j**, **Neo4j GDS**, **XGBoost**, **Drools**, and **DL4J** into a regulator-compliant AML decision engine.

---

## ✅ Pre-requisites (Completed)
- [x] XGBoost integration via `ScoringService.java`
- [x] Feature extraction service
- [x] Basic decision engine
- [x] Prometheus metrics integration
- [x] Aerospike caching for graph metrics & XGBoost scores (`AerospikeGraphCacheService.java`)
- [x] Cache read-through/write-through in `ScoringService`

---

## Phase 1: Neo4j Transaction Graph (Week 1-2)

### 1.1 Infrastructure
- [ ] Install Neo4j Community Edition
- [x] Add Spring Data Neo4j dependency to `pom.xml`
- [x] Configure `application.properties` for Neo4j connection

### 1.2 Graph Data Model
- [x] Create `MerchantNode.java` entity
- [x] Create `AccountNode.java` entity
- [x] Create `TransactionNode.java` entity
- [x] Create `DeviceNode.java` entity

### 1.3 Relationships
- [x] Define `TRANSACTS_WITH` relationship (Merchant → Merchant)
- [x] Define `SENDS_TO` relationship (Account → Account)
- [x] Define `USED_BY` relationship (Device → Account)
- [x] Define `OWNS_ACCOUNT` relationship (Merchant → Account)

### 1.4 Ingestion Service
- [x] Create `Neo4jGraphIngestionService.java`
- [x] Implement real-time transaction → graph node conversion
- [x] Implement relationship creation logic

---

## Phase 2: Neo4j GDS Graph Analytics (Week 3-4)

### 2.1 Setup
- [x] Add Neo4j GDS library dependency (via Spring Data Neo4j)
- [x] Create `Neo4jGdsService.java`

### 2.2 AML Graph Algorithms
- [x] Implement PageRank for entity influence scoring
- [x] Implement Community Detection (Louvain) for network clustering
- [x] Implement Betweenness Centrality for hub detection
- [x] Implement Shortest Path for money trail analysis (`findMoneyTrail`)
- [x] Implement Degree Centrality for connection counting
- [ ] Implement Triangle Count for network density (deferred)

### 2.3 Feature Export
- [x] Create graph metrics extraction method (`getGraphMetrics`)
- [x] Integrate with `FeatureExtractionService` (`extractGraphFeatures`)

---

## Phase 3: Enhanced XGBoost Scoring (Week 5-6)

### 3.1 Feature Engineering
- [x] Modify `FeatureExtractionService.java` to include graph features (done in Phase 2)
- [x] Add `pageRank` feature
- [x] Add `communityId` feature
- [x] Add `betweenness` feature
- [x] Add `connectionCount` feature
- [ ] Add `clusteringCoefficient` feature (deferred - requires triangle count)

### 3.2 Explainability
- [x] Create `XGBoostExplainabilityService.java`
- [x] Implement feature importance extraction (`getFeatureImportance`)
- [x] Log feature contributions for each decision (`explainScore`)
- [x] Store feature importance in Aerospike audit trail (`cacheExplanation`)
- [x] Generate human-readable reasons for regulators

---

## Phase 4: Drools Rules Engine (Week 7-8)

### 4.1 Setup
- [x] Add Drools dependencies to `pom.xml` (drools-core, drools-compiler, drools-mvel 8.44.0)
- [x] Create Drools configuration class (inline in Service)
- [x] Create `DroolsRulesService.java`

### 4.2 Rule Definitions
- [x] Create `aml-rules.drl` file
- [x] Implement CTR threshold rule ($10,000 USD)
- [x] Implement SAR structuring detection rule
- [x] Implement OFAC high-risk country block rule
- [x] Implement velocity-based rules
- [x] Implement ML score threshold rules
- [x] Implement Graph-based rules (Betweenness, PageRank)

### 4.3 Integration
- [x] Integrate rules engine with decision pipeline (via Aerospike caching)
- [x] Implement deterministic override logic (rules > ML)
- [ ] Implement rule versioning (deferred)

### 4.4 Aerospike Integration
- [x] Cache rule decisions in Aerospike for audit trails
- [x] Verified sanctions screening uses Aerospike (`AerospikeSanctionsScreeningService`)

---

## Phase 5: DL4J Deep Anomaly Detection (Week 9-10) - COMPLETE

### 5.1 Setup
- [x] Add DL4J dependencies to `pom.xml` (deeplearning4j-core, nd4j-native-platform)
- [x] Add ND4J native platform dependency
- [x] Create `DL4JAnomalyService.java`

### 5.2 Model
- [x] Implement autoencoder network architecture (20→14→8→14→20)
- [x] Implement feature-to-NDArray conversion (`featuresToNDArray`)
- [x] Implement reconstruction error calculation (`computeReconstructionError`)
- [x] Implement anomaly score thresholding

### 5.3 Integration
- [x] Integrate anomaly score with Aerospike caching
- [x] Add anomaly score to audit trail

---

## Phase 6: Grafana Dashboard Integration (Week 11-12)

### 6.1 Prometheus Metrics
- [x] Create `GraphMetricsExporter.java`
- [x] Export `aml_high_risk_communities` gauge
- [x] Export `aml_suspicious_hubs` gauge
- [x] Export `aml_score_distribution` gauge (by bucket)
- [x] Export `aml_drools_rule_triggers` counter
- [x] Export `aml_decision_count` gauge
- [x] Export `aml_scoring_latency` timer

### 6.2 Grafana Dashboards
- [x] Create `aml-graph-analytics.json` dashboard
- [x] Create High-Risk Communities panel
- [x] Create Suspicious Hub Detection panel
- [x] Create ML Score Distribution panel
- [x] Create Drools Rule Triggers panel
- [x] Create CTR/STR Submissions panel

---

## Regulatory Compliance Checklist

- [ ] **Explainability**: Document XGBoost feature importance extraction
- [ ] **Reproducibility**: Version control for Drools rule files
- [ ] **Audit Trails**: Complete decision logging with all scores
- [ ] **Deterministic Overrides**: Rules engine priority documentation
- [ ] **Documented Thresholds**: Externalized threshold configuration

---

## Testing Milestones

### Unit Tests
- [ ] Neo4j node/relationship tests
- [ ] Neo4j GDS algorithm tests
- [ ] Drools rule unit tests
- [ ] DL4J anomaly detection tests

### Integration Tests
- [ ] End-to-end decision pipeline test
- [ ] Graph ingestion → GDS → XGBoost flow test
- [ ] Rules engine integration test

### Performance Tests
- [ ] Graph query latency benchmarks
- [ ] ML scoring throughput test
- [ ] Rules engine evaluation time test

---

## Notes
- Mark items with `[x]` when completed
- Add date completed in comments if needed
- Update this file as we progress through phases
