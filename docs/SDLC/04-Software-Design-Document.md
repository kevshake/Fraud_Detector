# Software Design Document
## AML Fraud Detector System

**Version:** 1.0  
**Date:** January 2026  
**Status:** Approved

---

## 1. Introduction

### 1.1 Purpose
This Software Design Document (SDD) describes the technical design and implementation details of the AML Fraud Detector system.

### 1.2 Scope
This document covers:
- System architecture design
- Component design
- Data models
- Interface specifications
- Design patterns used

---

## 2. Architectural Design

### 2.1 Design Philosophy
The system follows these architectural principles:

1. **Layered Architecture**: Clear separation of concerns
2. **Domain-Driven Design**: Business logic organized by domain
3. **Configuration-Driven**: No hardcoded values
4. **Resilient by Design**: Circuit breakers, retries, fallbacks
5. **Observable**: Comprehensive logging and metrics

### 2.2 Layer Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐  │
│  │   REST Controllers  │  │   Static Web Assets (HTML/JS)   │  │
│  └─────────────────────┘  └─────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                     BUSINESS LOGIC LAYER                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │    Services      │  │   Orchestrators  │  │  Validators  │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                     DATA ACCESS LAYER                           │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │  JPA Repositories│  │ Aerospike Client │  │ Redis Client │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                     INFRASTRUCTURE LAYER                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │    PostgreSQL    │  │    Aerospike     │  │     Redis    │  │
│  └──────────────────┘  └──────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.3 Package Structure

```
com.posgateway.aml/
│
├── AmlFraudDetectorApplication.java    # Spring Boot main class
│
├── config/                              # Configuration classes
│   ├── SecurityConfig.java             # Spring Security
│   ├── CacheConfig.java                # Caching configuration
│   ├── AsyncConfig.java                # Async processing
│   ├── ResilienceConfig.java           # Circuit breakers
│   ├── WebConfig.java                  # Web/CORS configuration
│   └── PspLoggingFilter.java           # PSP-based logging filter (MDC injection)
│
├── controller/                          # REST API endpoints
│   ├── TransactionController.java      # Transaction APIs
│   ├── AlertController.java            # Alert APIs
│   ├── MerchantController.java         # Merchant APIs
│   ├── UserController.java             # User management APIs
│   ├── auth/                           # Authentication controllers
│   ├── analytics/                      # Analytics controllers
│   ├── compliance/                     # Compliance controllers
│   └── case_management/                # Case management controllers
│
├── service/                             # Business logic
│   ├── DecisionEngine.java             # Core decision logic
│   ├── ScoringService.java             # ML scoring orchestration
│   ├── FraudDetectionService.java      # Fraud detection
│   ├── AmlService.java                 # AML risk assessment
│   ├── TransactionIngestionService.java # Transaction processing
│   ├── risk/                           # Risk scoring services
│   │   ├── KycRiskScoreService.java    # KYC Risk Score (KRS)
│   │   ├── TransactionRiskScoreService.java # Transaction Risk Score (TRS)
│   │   ├── CustomerRiskAssessmentService.java # Customer Risk Assessment (CRA)
│   │   └── CustomerRiskProfilingService.java # Customer risk profiling
│   ├── deeplearning/                   # Deep learning services
│   │   └── DL4JAnomalyService.java     # Anomaly detection (autoencoder)
│   ├── case_management/                # Case workflow services
│   ├── sanctions/                      # Sanctions screening
│   ├── analytics/                      # Analytics services
│   ├── compliance/                     # Compliance services
│   │   └── AuditReportService.java     # Audit report generation
│   └── psp/                            # PSP-related services
│       └── RequestCounter.java         # Rate limiting counter
│
├── entity/                              # JPA entities
│   ├── TransactionEntity.java
│   ├── Alert.java
│   ├── User.java
│   ├── merchant/                       # Merchant domain entities
│   └── compliance/                     # Compliance domain entities
│
├── repository/                          # Data access
│   ├── TransactionRepository.java
│   ├── AlertRepository.java
│   └── ...
│
├── dto/                                 # Data transfer objects
│   ├── request/                        # Request DTOs
│   └── response/                       # Response DTOs
│
├── mapper/                              # MapStruct mappers
│   ├── SarMapper.java
│   └── FraudDetectionMapper.java
│
└── exception/                           # Custom exceptions
    └── GlobalExceptionHandler.java
```

---

## 3. Component Design

### 3.1 Transaction Processing Component

#### 3.1.1 Class Diagram

```
┌─────────────────────────────┐
│   TransactionController     │
└─────────────┬───────────────┘
              │
              ▼
┌─────────────────────────────┐
│ TransactionIngestionService │
└─────────────┬───────────────┘
              │
    ┌─────────┼─────────┐
    ▼         ▼         ▼
┌────────┐ ┌────────┐ ┌────────────────┐
│Feature │ │Scoring │ │ Decision       │
│Extraction│ │Service │ │ Engine         │
└────────┘ └────────┘ └────────────────┘
```

#### 3.1.2 Sequence Diagram

```
Client          Controller        IngestionSvc      FeatureExtract    ScoringService    DecisionEngine
   │                │                  │                 │                  │                │
   │──POST /ingest──▶                  │                 │                  │                │
   │                │──processTransaction──▶             │                  │                │
   │                │                  │──extractFeatures──▶                │                │
   │                │                  │◀─────features────┤                 │                │
   │                │                  │─────────────callScoringService─────▶                │
   │                │                  │◀─────────────score─────────────────┤                │
   │                │                  │────────────────applyRules──────────────────────────▶│
   │                │                  │◀───────────────decision────────────────────────────┤
   │                │◀────response─────┤                 │                  │                │
   │◀───JSON────────┤                  │                 │                  │                │
```

### 3.2 AML Screening Component

#### 3.2.1 Screening Pipeline

```java
@Service
public class AmlService {
    
    public AmlRiskAssessment assess(Transaction txn) {
        List<RiskFactor> factors = new ArrayList<>();
        
        // 1. Amount-based risk
        factors.addAll(assessAmountRisk(txn));
        
        // 2. Velocity-based risk
        factors.addAll(assessVelocityRisk(txn));
        
        // 3. Geographic risk
        factors.addAll(assessGeographicRisk(txn));
        
        // 4. Pattern detection
        factors.addAll(detectPatterns(txn));
        
        // 5. Sanctions screening
        factors.addAll(screenSanctions(txn));
        
        return aggregateRiskScore(factors);
    }
}
```

### 3.3 Scoring Services Component

#### 3.3.1 Multi-Layered Scoring Architecture

The system implements multiple scoring systems that work together:

```java
@Service
public class ScoringService {
    // Orchestrates ML scoring, rule evaluation, and anomaly detection
    public ScoringResult scoreTransaction(String txnId, Map<String, Object> features) {
        // 1. ML Score from XGBoost
        double mlScore = mlScoringClient.score(features);
        
        // 2. Anomaly detection
        double anomalyScore = dl4jAnomalyService.detectAnomaly(features);
        
        // 3. Rule evaluation (may override ML score)
        RuleDecision ruleDecision = droolsRulesService.evaluate(txnId, mlScore);
        
        return new ScoringResult(mlScore, anomalyScore, ruleDecision);
    }
}

@Service
public class KycRiskScoreService {
    // Calculates KYC Risk Score (KRS) using weighted averages
    public KycRiskScoreResult calculateBusinessKrs(String merchantId) {
        // Weighted average: KRS = Σ(component × weight) / Σ(weights)
        // Components: country registration, director/UBO nationality, 
        //             business age, business domain
    }
}

@Service
public class TransactionRiskScoreService {
    // Calculates Transaction Risk Score (TRS) using weighted averages
    public TransactionRiskScoreResult calculateTrs(TransactionEntity txn) {
        // Weighted average: TRS = Σ(component × weight) / Σ(weights)
        // Components: payment origin/destination, payment method, 
        //             receiver merchant, transaction amount
    }
}

@Service
public class CustomerRiskAssessmentService {
    // Calculates dynamic Customer Risk Assessment (CRA)
    public CustomerRiskAssessmentResult calculateCra(String merchantId) {
        // CRA[0] = KRS
        // CRA[i] = (CRA[i-1] + TRS[i]) / 2
    }
}
```

**Score Calculation Details:**
For complete formulas, component calculations, and examples, see **[SCORING_PROCESS_DOCUMENTATION.md](SCORING_PROCESS_DOCUMENTATION.md)**.

#### 3.3.2 Decision Engine Component

#### 3.3.2.1 Decision Logic

```java
public class DecisionEngine {
    
    public TransactionDecision evaluate(TransactionContext ctx) {
        // Phase 1: Hard rules (immediate block)
        if (isBlacklisted(ctx.getPan())) {
            return Decision.BLOCK("PAN blacklisted");
        }
        
        // Phase 2: ML Score-based rules
        if (ctx.getScore() >= blockThreshold) {
            return Decision.BLOCK("Score exceeds block threshold");
        }
        if (ctx.getScore() >= holdThreshold) {
            return Decision.HOLD("Score exceeds hold threshold");
        }
        
        // Phase 3: AML rules
        if (ctx.getAmlRiskScore() >= amlAlertThreshold) {
            return Decision.ALERT("AML risk detected");
        }
        
        return Decision.ALLOW();
    }
}
```

### 3.4 Case Management Component

#### 3.4.1 Service Components

The case management system includes the following services:

- **CaseCreationService**: Creates cases from various triggers (rules, ML, sanctions, graph anomalies)
- **CaseEnrichmentService**: Enriches cases with related context (transactions, merchants, risk details)
- **CaseAssignmentService**: Workload-based and round-robin case assignment
- **CaseQueueService**: Queue management with auto-assignment
- **CaseTimelineService**: Chronological event timeline generation
- **CaseNetworkService**: Network graph generation for related entities
- **CaseEscalationService**: Automatic and manual escalation logic
- **CaseSlaService**: SLA deadline tracking and breach detection
- **CaseDecisionService**: Case decision workflow and status updates

#### 3.4.1.1 Case Enrichment Service Design

```java
@Service
public class CaseEnrichmentService {
    // Async enrichment to avoid blocking case creation
    @Async
    public void enrichWithTransaction(ComplianceCase cCase, TransactionEntity tx, String relationshipType);
    
    @Async
    public void enrichWithMerchantProfile(ComplianceCase cCase, Long merchantId);
    
    @Async
    public void enrichWithRiskDetails(ComplianceCase cCase, Map<String, Object> riskDetails);
}
```

**Enrichment Flow:**
1. Case created → Enrichment triggered asynchronously
2. Transaction linking → Creates CaseTransaction records
3. Merchant profile linking → Creates CaseEntity records, triggers KYC checks
4. Graph context update → Updates Neo4j merchant risk status
5. Risk details attachment → Creates structured case notes

#### 3.4.1.2 Graph Anomaly Detection Integration

The system integrates with Neo4j Graph Data Science (GDS) for anomaly detection:

- **detectCycles()**: Identifies circular trading patterns (money loops)
- **detectMuleProximity()**: Detects proximity to high-risk entities
- **updateMerchantRiskStatus()**: Updates merchant risk scores in graph

Anomalies detected trigger cases via `CaseCreationService.triggerCaseFromGraph()`.

#### 3.4.2 State Machine

```
                    ┌─────────┐
                    │   NEW   │
                    └────┬────┘
                         │ assign()
                         ▼
                    ┌─────────┐
                    │ASSIGNED │
                    └────┬────┘
                         │ startInvestigation()
                         ▼
                ┌────────────────┐
                │ INVESTIGATING  │
                └───────┬────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌───────────┐   ┌─────────────┐   ┌──────────┐
│  RESOLVED │   │ PENDING_    │   │ESCALATED │
│           │   │ REVIEW      │   │          │
└───────────┘   └──────┬──────┘   └────┬─────┘
                       │               │
                       ▼               ▼
                 ┌──────────┐    ┌──────────┐
                 │  CLOSED  │    │   SAR    │
                 └──────────┘    └──────────┘
```

### 3.5 Security and Logging Components

#### 3.5.1 PSP Logging Filter

The `PspLoggingFilter` is a critical security component that enables PSP-based log segregation by injecting the current user's PSP ID into the Mapped Diagnostic Context (MDC) for all log statements.

**Purpose:**
- Enable log clustering and filtering by PSP ID
- Support multi-tenant log analysis
- Facilitate PSP-specific audit trails
- Improve security incident investigation

**Implementation:**

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 101) // Run after Spring Security filter chain
public class PspLoggingFilter extends OncePerRequestFilter {
    
    private final PspIsolationService pspIsolationService;
    
    public static final String MDC_KEY_PSP_ID = "pspId";
    public static final String SUPER_ADMIN_PSP_NAME = "super_admin";
    public static final String UNKNOWN_PSP = "unknown";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        try {
            // Inject PSP ID into MDC based on authenticated user
            if (pspIsolationService.isPlatformAdministrator()) {
                MDC.put(MDC_KEY_PSP_ID, SUPER_ADMIN_PSP_NAME);
            } else {
                String pspCode = pspIsolationService.getCurrentUserPspCode();
                MDC.put(MDC_KEY_PSP_ID, pspCode != null ? pspCode : UNKNOWN_PSP);
            }
            
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent data leaking
            MDC.remove(MDC_KEY_PSP_ID);
        }
    }
}
```

**Key Features:**
- **Execution Order**: Runs after Spring Security filter chain (order: HIGHEST_PRECEDENCE + 101)
- **MDC Injection**: Automatically adds PSP ID to all log statements within the request scope
- **Super Admin Handling**: Uses special "super_admin" identifier for platform administrators
- **Fallback Handling**: Uses "unknown" for unauthenticated or error scenarios
- **Thread Safety**: Properly cleans up MDC in finally block to prevent thread pool contamination

**Usage in Logging:**
```java
// Logback pattern configuration
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [PSP:%X{pspId}] - %msg%n</pattern>

// Example log output
2026-01-16 09:00:00 [http-nio-8080-exec-1] INFO  c.p.a.controller.AlertController [PSP:PSP_BANK_A] - Fetching alerts for PSP
```

**Security Considerations:**
- Filter must run AFTER Spring Security to access authenticated user context
- MDC cleanup is critical to prevent cross-request data leakage in thread pools
- Exception handling ensures MDC is always cleaned up, even on errors

---

#### 3.5.2 Request Counter (Rate Limiting)

The `RequestCounter` class provides thread-safe, time-windowed request counting for rate limiting purposes.

**Purpose:**
- Implement per-PSP rate limiting
- Prevent API abuse and DoS attacks
- Enforce fair usage policies
- Protect backend resources

**Implementation:**

```java
public class RequestCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile long windowStart = Instant.now().getEpochSecond() / 60; // Minute window
    
    /**
     * Attempts to acquire a request slot within the current time window.
     * 
     * @param maxRequests Maximum allowed requests per minute
     * @return true if request is allowed, false if rate limit exceeded
     */
    public synchronized boolean tryAcquire(int maxRequests) {
        long currentWindow = Instant.now().getEpochSecond() / 60;
        
        // Reset counter if we've moved to a new time window
        if (currentWindow > windowStart) {
            windowStart = currentWindow;
            count.set(0);
        }
        
        // Check if we're within the limit
        return count.incrementAndGet() <= maxRequests;
    }
}
```

**Key Features:**
- **Time Window**: Uses 1-minute rolling windows (60 seconds)
- **Thread Safety**: Synchronized method ensures atomic window checks and counter updates
- **Atomic Operations**: Uses `AtomicInteger` for lock-free counter increments
- **Automatic Reset**: Counter automatically resets when moving to a new time window
- **Simple API**: Single `tryAcquire()` method for easy integration

**Usage Pattern:**

```java
@Service
public class PspRateLimitService {
    // Map of PSP ID to RequestCounter
    private final ConcurrentHashMap<Long, RequestCounter> pspCounters = new ConcurrentHashMap<>();
    
    public boolean isAllowed(Long pspId, int maxRequestsPerMinute) {
        RequestCounter counter = pspCounters.computeIfAbsent(pspId, k -> new RequestCounter());
        return counter.tryAcquire(maxRequestsPerMinute);
    }
}

// In controller or filter
if (!rateLimitService.isAllowed(pspId, 1000)) {
    throw new RateLimitExceededException("Too many requests. Please try again later.");
}
```

**Design Decisions:**
- **Minute-based Windows**: Balances granularity with simplicity
- **Synchronized Method**: Chosen over locks for simplicity and correctness
- **Volatile Window Start**: Ensures visibility across threads
- **No External Dependencies**: Self-contained, no Redis/cache required for basic rate limiting

**Scalability Considerations:**
- For distributed systems, consider Redis-based rate limiting
- Current implementation is suitable for single-instance deployments
- Can be extended with distributed counters (e.g., Redis INCR with TTL)

---

#### 3.5.3 Audit Report Service

The `AuditReportService` generates comprehensive audit reports and user activity reports for compliance and regulatory purposes.

**Purpose:**
- Generate regulatory audit reports
- Track user activity for compliance
- Support forensic investigations
- Provide audit trail analytics

**Key Methods:**

```java
@Service
public class AuditReportService {
    
    /**
     * Generate comprehensive audit report for a date range
     */
    public AuditReport generateAuditReport(LocalDateTime startDate, LocalDateTime endDate) {
        // Fetch audit logs from repository
        List<AuditTrail> auditLogs = auditTrailRepository.findByPerformedAtBetween(startDate, endDate);
        
        // Aggregate statistics
        AuditReport report = new AuditReport();
        report.setTotalEvents(auditLogs.size());
        report.setEventsByActionType(groupByActionType(auditLogs));
        report.setEventsByUser(groupByUser(auditLogs));
        report.setEventsByEntityType(groupByEntityType(auditLogs));
        
        return report;
    }
    
    /**
     * Generate user-specific activity report
     */
    public UserActivityReport generateUserActivityReport(String userId, 
                                                         LocalDateTime startDate, 
                                                         LocalDateTime endDate) {
        // Filter logs by user
        List<AuditTrail> userLogs = auditTrailRepository
            .findByPerformedAtBetween(startDate, endDate)
            .stream()
            .filter(log -> userId.equals(log.getPerformedBy()))
            .toList();
        
        // Generate user-specific statistics
        UserActivityReport report = new UserActivityReport();
        report.setUserId(userId);
        report.setTotalActions(userLogs.size());
        report.setActionsByType(groupByActionType(userLogs));
        
        return report;
    }
}
```

**Report DTOs:**

```java
public static class AuditReport {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime generatedAt;
    private long totalEvents;
    private int uniqueUsers;
    private int uniqueEntityTypes;
    private Map<String, Long> eventsByActionType;
    private Map<String, Long> eventsByUser;
    private Map<String, Long> eventsByEntityType;
}

public static class UserActivityReport {
    private String userId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private long totalActions;
    private Map<String, Long> actionsByType;
}
```

**Integration Points:**
- **AuditTrailRepository**: Fetches audit trail data from PostgreSQL
- **CasePermissionService**: Ensures PSP-based data isolation (future enhancement)
- **REST Controllers**: Exposed via `/api/v1/audit/reports/*` endpoints

**PSP Isolation:**
- Reports automatically filter data based on user's PSP ID
- Super Admin users can generate cross-PSP reports
- PSP users can only access their own PSP's audit data

---

## 4. Data Design

### 4.1 Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│   Merchant  │──────<│ Transaction │>──────│   Alert     │
└─────────────┘       └─────────────┘       └──────┬──────┘
      │                      │                     │
      │                      │                     │
      ▼                      ▼                     ▼
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│ Beneficial  │       │ Transaction │       │    Case     │
│  Owner      │       │  Features   │       └──────┬──────┘
└─────────────┘       └─────────────┘              │
                                                   ▼
                                            ┌─────────────┐
                                            │  SAR Report │
                                            └─────────────┘

┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    User     │──────<│    Role     │>──────│ Permission  │
└─────────────┘       └─────────────┘       └─────────────┘
      │
      ▼
┌─────────────┐
│  AuditLog   │
└─────────────┘
```

### 4.2 Key Entity Specifications

#### Transaction Entity
```java
@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id @GeneratedValue
    private Long id;
    
    @Column(nullable = false)
    private String panHash;  // SHA-256 tokenized
    
    @Column(nullable = false)
    private String merchantId;
    
    private String terminalId;
    
    @Column(nullable = false)
    private Long amountCents;
    
    @Column(length = 3)
    private String currency;
    
    private LocalDateTime txnTimestamp;
    
    @Column(columnDefinition = "jsonb")
    private String emvTags;  // JSON
    
    private Double fraudScore;
    
    @Enumerated(EnumType.STRING)
    private DecisionAction decision;
    
    private LocalDateTime createdAt;
}
```

#### User Entity
```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    private String passwordHash;
    
    private String firstName;
    private String lastName;
    
    @ManyToOne
    private Role role;
    
    private boolean active;
    private boolean locked;
    
    private LocalDateTime lastLogin;
    private int failedLoginAttempts;
    
    private Long pspId;  // Multi-tenancy support
}
```

#### ComplianceCase Entity
```java
@Entity
@Table(name = "compliance_cases", indexes = {
    @Index(name = "idx_case_merchant", columnList = "merchant_id"),
    @Index(name = "idx_case_psp", columnList = "psp_id"),
    @Index(name = "idx_case_status", columnList = "status"),
    @Index(name = "idx_case_created", columnList = "createdAt")
})
@Audited
public class ComplianceCase {
    @Id @GeneratedValue
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String caseReference;  // e.g., CASE-2023-0001
    
    private Long merchantId;
    private Long pspId;  // Multi-tenancy
    
    @Enumerated(EnumType.STRING)
    private CaseStatus status;
    
    @Enumerated(EnumType.STRING)
    private CasePriority priority;
    
    private LocalDateTime slaDeadline;
    private Integer daysOpen;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private User assignedTo;
    
    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL)
    private List<CaseAlert> alerts;  // Versioned alerts
    
    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL)
    private List<CaseTransaction> transactions;  // Enrichment links
    
    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL)
    private List<CaseEntity> entities;  // Merchant/customer links
    
    // Archival & Retention
    private Boolean archived = false;
    private LocalDateTime archivedAt;
    private String archiveReference;  // Cold storage path/ARN
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private LocalDateTime createdAt;
}
```

---

## 5. Interface Design

### 5.1 REST API Design Pattern

All APIs follow these conventions:
- Base path: `/api/v1`
- JSON content type
- Standard HTTP status codes
- Consistent error response format

#### Error Response Format
```json
{
    "timestamp": "2026-01-09T08:00:00Z",
    "status": 400,
    "error": "Bad Request",
    "errorCode": "VALIDATION_ERROR",
    "message": "Invalid input",
    "details": ["amount must be positive"],
    "traceId": "abc123xyz"
}
```

### 5.2 Key API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/transactions/ingest` | POST | Ingest transaction |
| `/api/v1/alerts` | GET | List alerts |
| `/api/v1/compliance/cases` | GET | List cases |
| `/api/v1/merchants` | GET/POST | Merchant CRUD |
| `/api/v1/users` | GET/POST | User management |

---

## 6. Design Patterns Used

### 6.1 Patterns Overview

| Pattern | Usage |
|---------|-------|
| **Singleton** | AerospikeConnectionService |
| **Strategy** | Scoring strategies, Risk calculators |
| **Factory** | Alert creation, Report generation |
| **Observer** | Event publishing (case updates) |
| **Circuit Breaker** | External service calls |
| **Builder** | Complex DTOs, Search criteria |
| **Repository** | Data access abstraction |
| **Decorator** | Caching wrappers |

### 6.2 Example: Strategy Pattern

```java
public interface RiskAssessor {
    RiskScore assess(Transaction txn);
}

@Component
public class AmountRiskAssessor implements RiskAssessor {
    public RiskScore assess(Transaction txn) {
        // Amount-based risk logic
    }
}

@Component
public class VelocityRiskAssessor implements RiskAssessor {
    public RiskScore assess(Transaction txn) {
        // Velocity-based risk logic
    }
}

@Service
public class AmlService {
    private final List<RiskAssessor> assessors;
    
    public RiskScore aggregateRisk(Transaction txn) {
        return assessors.stream()
            .map(a -> a.assess(txn))
            .reduce(RiskScore::combine)
            .orElse(RiskScore.NONE);
    }
}
```

---

## 7. Security Design

### 7.1 Authentication

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
            )
            .build();
    }
}
```

### 7.2 Authorization Matrix

| Resource | VIEWER | ANALYST | ADMIN | SUPER_ADMIN |
|----------|--------|---------|-------|-------------|
| View Dashboard | ✓ | ✓ | ✓ | ✓ |
| View Transactions | ✓ | ✓ | ✓ | ✓ |
| Create Alerts | | ✓ | ✓ | ✓ |
| Manage Cases | | ✓ | ✓ | ✓ |
| File SARs | | ✓ | ✓ | ✓ |
| Manage Users | | | ✓ | ✓ |
| System Config | | | | ✓ |

---

## 8. Performance Design

### 8.1 Caching Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                    REQUEST FLOW                              │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  L1 CACHE: Caffeine (In-Memory)                             │
│  TTL: 5 minutes | Size: 10,000 entries                      │
│  Use: Hot data (thresholds, config)                         │
└────────────────────────────┬────────────────────────────────┘
                    Cache Miss │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  L2 CACHE: Redis                                            │
│  TTL: 1 hour | Use: Statistics, session data                │
└────────────────────────────┬────────────────────────────────┘
                    Cache Miss │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  L3 CACHE: Aerospike                                        │
│  TTL: 24 hours | Use: Sanctions lists, features             │
└────────────────────────────┬────────────────────────────────┘
                    Cache Miss │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│  DATABASE: PostgreSQL                                        │
│  Source of truth                                             │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 Connection Pooling

| Pool | Size | Purpose |
|------|------|---------|
| HikariCP | 50-300 | Database connections |
| Apache HttpClient | 30,000 | External HTTP calls |
| Aerospike | 64 | Aerospike connections |
| Redis Lettuce | 16-64 | Redis connections |

---

## 9. Deployment Considerations

### 9.1 Configuration Externalization

All configuration via:
1. `application.properties` / `application.yml`
2. Environment variables
3. Database `model_config` table

### 9.2 Health Checks

```java
@Endpoint(id = "customhealth")
public class CustomHealthEndpoint {
    
    @ReadOperation
    public Map<String, Object> health() {
        return Map.of(
            "database", checkDatabase(),
            "aerospike", checkAerospike(),
            "redis", checkRedis(),
            "scoringService", checkScoringService()
        );
    }
}
```

---

## 10. Appendices

### A. Technology Decisions Log

| Decision | Options Considered | Choice | Rationale |
|----------|-------------------|--------|-----------|
| ORM | Hibernate, MyBatis | Hibernate | Spring integration, JPA standard |
| Cache | Redis only, Aerospike only | Both | Different use cases |
| Mapping | Manual, ModelMapper, MapStruct | MapStruct | Compile-time, type-safe |
| Resilience | Hystrix, Resilience4j | Resilience4j | Modern, actively maintained |

### B. Related Documents

- [01-Technical-Architecture.md](01-Technical-Architecture.md)
- [06-Database-Design.md](06-Database-Design.md)
- [05-API-Reference.md](05-API-Reference.md)
