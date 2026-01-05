# Application Runtime Understanding
**Date**: 2025-12-29  
**Purpose**: Internalize how the AML Fraud Detector application runs based on today's work

---

## 🚀 APPLICATION STARTUP FLOW

### 1. Main Entry Point
**File**: `AmlFraudDetectorApplication.java`
- Spring Boot application with `@SpringBootApplication`
- Enables: `@EnableConfigurationProperties`, `@EnableAsync`, `@EnableScheduling`
- Starts Spring context and auto-configuration

### 2. Initialization Sequence

#### Phase 1: Infrastructure Services (PostConstruct)
1. **AerospikeConnectionService** (`@PostConstruct`)
   - Reads configuration from `application.properties`
   - Connects to Aerospike cluster (if enabled)
   - Creates singleton `AerospikeClient` instance
   - Sets connection status flag
   - **Key Config**: `aerospike.enabled`, `aerospike.hosts`, `aerospike.namespace`

2. **AerospikeInitializationService** (`@PostConstruct`, conditional)
   - Waits for Aerospike connection (configurable wait time)
   - Verifies namespace exists
   - Creates secondary indexes automatically
   - Initializes sets with sample records if needed
   - **Key Config**: `aerospike.auto.init.enabled`, `aerospike.auto.init.wait.seconds`

3. **Cache Services Initialization**
   - `AerospikeCacheService` - Generic caching layer
   - `ScreeningCacheService` - Screening results caching
   - `KycDataCacheService` - KYC data caching
   - `DocumentAccessCacheService` - Document permissions caching
   - `AlertMetricsCacheService` - Rule metrics caching
   - All depend on `AerospikeConnectionService`

#### Phase 2: Scheduled Services (Scheduled Tasks)
Multiple services with `@Scheduled` annotations:

1. **SanctionsListDownloadService**
   - Daily download from OpenSanctions
   - Processes and loads to Aerospike
   - Tracks update frequency via `WatchlistUpdateTrackingService`
   - **Schedule**: Daily (configurable cron)

2. **PeriodicRescreeningService**
   - Ongoing rescreening of merchants
   - Risk-based rescreening frequency
   - **Schedule**: Configurable interval

3. **PeriodicKycRefreshService**
   - Risk-based KYC refresh
   - Triggers based on merchant risk level
   - **Schedule**: Configurable interval

4. **KycExpirationTrackingService**
   - Tracks expiring KYC documents
   - Sends alerts before expiration
   - **Schedule**: Daily checks

5. **DocumentRetentionService**
   - Enforces document retention policies
   - Auto-deletes expired documents
   - **Schedule**: Daily cleanup

6. **CaseEscalationService**
   - Escalates overdue cases
   - Checks SLA breaches
   - **Schedule**: Hourly checks

7. **WorkflowAutomationService**
   - Auto-approves low-risk merchants
   - Auto-assigns cases
   - Escalates overdue cases
   - **Schedule**: Hourly escalation checks

8. **ComplianceCalendarService**
   - Tracks regulatory deadlines
   - Sends deadline reminders
   - **Schedule**: Daily checks

9. **EnhancedAuditService**
   - Audit log retention enforcement
   - **Schedule**: Periodic cleanup

10. **Http2HealthMonitorService**
    - Monitors HTTP/2 health
    - Auto-detects HTTP/2 support
    - **Schedule**: Configurable intervals

11. **Http2NetworkStabilityService**
    - Monitors network stability
    - **Schedule**: Configurable intervals

---

## 🔄 REQUEST PROCESSING FLOW

### Real-Time Transaction Screening Flow

1. **Transaction Received** → `TransactionController` or similar
2. **RealTimeTransactionScreeningService.screenTransaction()**
   - Checks if real-time screening enabled
   - Fast Aerospike cache lookup for whitelist
   - If whitelisted → return clear result
   - If not whitelisted:
     - Check cache for previous screening result
     - If cached → return cached result
     - If not cached:
       - Call `AerospikeSanctionsScreeningService.screenName()`
       - Query Aerospike using phonetic codes
       - Calculate similarity scores
       - Cache result in Aerospike
       - Return result
3. **Decision Engine** (if match found)
   - Blocks transaction if configured
   - Creates alert if configured
   - Logs to audit trail

### Merchant Onboarding Flow

1. **MerchantOnboardingService.onboardMerchant()**
   - Creates merchant entity
   - Calls `AmlScreeningOrchestrator.screenMerchantWithOwners()`
   - Screens merchant legal name
   - Screens beneficial owners
   - Calculates risk score
   - Makes decision (APPROVE/REVIEW/REJECT)
   - Creates compliance case if needed
   - Creates audit trail
   - **WorkflowAutomationService** auto-approves if low risk

### KYC Completeness Flow

1. **KycCompletenessService.calculateCompletenessScore()**
   - Fast Aerospike cache lookup first
   - If cached → return cached score
   - If not cached:
     - Calculate document completeness (40% weight)
     - Calculate owner completeness (30% weight)
     - Calculate basic info completeness (30% weight)
     - Calculate overall weighted score
     - Cache score in Aerospike
     - Return score

### Document Access Flow

1. **DocumentAccessControlService.canAccessDocument()**
   - Fast Aerospike cache lookup for permission
   - If cached → return cached permission
   - If not cached:
     - Check role-based access (ADMIN, COMPLIANCE_OFFICER, PSP_ADMIN, PSP_USER)
     - Cache permission in Aerospike
     - Return permission
   - Log access via `DocumentAccessLog`

### Alert Metrics Flow

1. **RuleEffectivenessService.getRuleEffectiveness()**
   - Fast Aerospike cache lookup for metrics
   - If cached → return cached metrics
   - If not cached:
     - Query alerts from database
     - Calculate true positives, false positives, precision
     - Cache metrics in Aerospike
     - Return metrics

---

## 💾 DATA STORAGE ARCHITECTURE

### PostgreSQL (Primary Database)
- **Purpose**: Persistent storage for all entities
- **Entities**: Merchants, Transactions, Alerts, Cases, Documents, SARs, Audit logs, etc.
- **Connection Pool**: HikariCP (100-300 connections, configurable)
- **JPA**: Hibernate with batch processing enabled

### Aerospike (High-Performance Cache)
- **Purpose**: Ultra-fast lookups and caching
- **Namespace**: `sanctions` (default) or `test` (configurable)
- **Data Stored**:
  - **Sanctions Lists**: All watchlist entities (set: `entities`)
  - **Screening Results**: Cached screening results (set: `screening_results`)
  - **Whitelist Entries**: False positive whitelist (set: `screening_whitelist`)
  - **Override Entries**: Screening overrides (set: `screening_overrides`)
  - **Custom Watchlists**: Custom watchlist entries (set: `custom_watchlists`)
  - **KYC Completeness**: Completeness scores (set: `kyc_completeness`)
  - **Risk Ratings**: Risk level and scores (set: `risk_ratings`)
  - **KYC Expiration**: Expiration dates (set: `kyc_expiration`)
  - **Document Access**: Access permissions (set: `document_access`)
  - **Document Permissions**: Permission maps (set: `document_permissions`)
  - **Rule Metrics**: Rule effectiveness metrics (set: `rule_metrics`)
  - **Rule Effectiveness**: Effectiveness summaries (set: `rule_effectiveness`)

### Cache Strategy
- **TTL**: Configurable per cache type (defaults: 1-24 hours)
- **Fallback**: If Aerospike unavailable, falls back to PostgreSQL
- **Invalidation**: Manual invalidation methods available

---

## 🔍 KEY SERVICES AND THEIR ROLES

### Screening Services
1. **RealTimeTransactionScreeningService** - Real-time transaction screening
2. **AerospikeSanctionsScreeningService** - Fast Aerospike-based screening
3. **ScreeningWhitelistService** - False positive management
4. **ScreeningOverrideService** - Override workflow
5. **ScreeningCoverageService** - Coverage statistics
6. **CustomWatchlistService** - Custom watchlist management
7. **NewListMatchAlertService** - New match alerts
8. **WatchlistUpdateTrackingService** - Update tracking

### KYC Services
1. **KycCompletenessService** - Completeness scoring
2. **PeriodicKycRefreshService** - Scheduled refresh
3. **TriggerBasedKycService** - Event-triggered updates
4. **KycExpirationTrackingService** - Expiration tracking

### Document Services
1. **DocumentVersionService** - Version control
2. **DocumentRetentionService** - Retention enforcement
3. **DocumentAccessControlService** - Access control
4. **DocumentSearchService** - Search functionality

### Alert Services
1. **FalsePositiveFeedbackService** - Feedback collection
2. **RuleEffectivenessService** - Performance tracking
3. **AlertTuningService** - ML-based tuning
4. **RuleAbTestingService** - A/B testing

### Case Management Services
1. **CaseActivityService** - Activity feed
2. **CaseAssignmentService** - Workload-based assignment
3. **CaseSlaService** - SLA tracking
4. **CaseEscalationService** - Escalation handling
5. **CaseQueueService** - Queue management
6. **CaseTimelineService** - Timeline generation
7. **CaseNetworkService** - Network graph generation

### Analytics Services
1. **BehavioralAnalyticsService** - Peer comparison, dormant account detection
2. **BehavioralProfilingService** - Baseline profiling
3. **RiskAnalyticsService** - Risk heatmaps, trends
4. **ComplianceDashboardService** - Real-time metrics

### AML Detection Services
1. **AmlScenarioDetectionService** - Pattern detection (structuring, rapid movement, funnel accounts, trade-based ML)

---

## ⚙️ CONFIGURATION KEY POINTS

### Application Properties
- **Server**: Port 8080, context path `/api/v1`
- **Tomcat**: High throughput (200-1000 threads, 10K connections)
- **HTTP/2**: Auto-detection and failover enabled
- **Database**: PostgreSQL with HikariCP pool
- **Aerospike**: Configurable hosts, namespace, security
- **Scheduling**: Multiple scheduled tasks for periodic operations
- **Caching**: TTLs configurable per cache type

### Environment Variables
- `SERVER_PORT` - Server port (default: 8080)
- `DATABASE_URL` - PostgreSQL connection string
- `DATABASE_USERNAME` - Database username
- `POSTGRES_DATABASE_PASSWORD` - Database password
- `AEROSPIKE_ENABLED` - Enable/disable Aerospike
- `AEROSPIKE_HOSTS` - Aerospike hosts (comma-separated)
- `AEROSPIKE_NAMESPACE` - Aerospike namespace
- `AEROSPIKE_SECURITY_ENABLED` - Enable Aerospike security
- `AEROSPIKE_USERNAME` - Aerospike username
- `AEROSPIKE_PASSWORD` - Aerospike password

---

## 🔄 LIFECYCLE MANAGEMENT

### Application Startup
1. Spring Boot context initialization
2. Infrastructure services initialization (`@PostConstruct`)
3. Aerospike connection establishment
4. Scheduled tasks registration
5. Application ready

### Application Shutdown
1. Scheduled tasks stop
2. Aerospike connection cleanup (`@PreDestroy`)
3. Database connection pool shutdown
4. Spring context shutdown

### Health Checks
- Actuator endpoints: `/actuator/health`
- Aerospike connection health check
- Database connection health check
- Service-specific health endpoints

---

## 📊 PERFORMANCE CHARACTERISTICS

### High Throughput Design
- **Tomcat**: 200-1000 threads, 10K max connections
- **HikariCP**: 100-300 connection pool
- **Aerospike**: 300 max connections per node
- **Batch Processing**: JPA batch size 200
- **Caching**: Multi-layer caching (Aerospike + Spring Cache)

### Scalability
- **Horizontal**: Stateless design, can scale horizontally
- **Vertical**: High thread/connection limits for vertical scaling
- **Caching**: Reduces database load significantly

---

## 🎯 KEY TAKEAWAYS

1. **Dual Storage**: PostgreSQL for persistence, Aerospike for speed
2. **Cache-First Strategy**: Always check cache before database
3. **Graceful Degradation**: Falls back to PostgreSQL if Aerospike unavailable
4. **Scheduled Operations**: Many background tasks for maintenance
5. **Workflow Automation**: Auto-approval, auto-assignment, auto-escalation
6. **Comprehensive Logging**: Audit trail for all operations
7. **High Performance**: Designed for 30K+ requests/second

---

## 📝 OPERATIONAL NOTES

### Monitoring
- Prometheus metrics available
- Actuator endpoints for health checks
- Comprehensive logging throughout

### Error Handling
- Graceful fallbacks when services unavailable
- Comprehensive error logging
- Retry mechanisms for external services

### Security
- Role-based access control
- Document access control
- Audit trail for all operations
- Aerospike security support

---

**Last Updated**: 2025-12-29  
**Status**: Complete understanding of application runtime

