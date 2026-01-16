# Case Management Features - Implementation Research

This document provides detailed implementation guidance for the missing case management features identified in `aml_feature_gap_analysis.md` (lines 91-126).

---

## 1. Case Lifecycle Management

### 1.1 Workflow State Machine Implementation

**Current State:** Basic `CaseStatus` enum exists, but no state transition validation or workflow enforcement.

**Implementation Approach:**

#### Option A: Spring State Machine (Recommended)
Use Spring State Machine framework for robust workflow management:

```java
// Maven Dependency
<dependency>
    <groupId>org.springframework.statemachine</groupId>
    <artifactId>spring-statemachine-core</artifactId>
    <version>3.2.0</version>
</dependency>
```

**State Machine Configuration:**
```java
@Configuration
@EnableStateMachine
public class CaseWorkflowStateMachineConfig extends StateMachineConfigurerAdapter<CaseStatus, CaseEvent> {
    
    @Override
    public void configure(StateMachineStateConfigurer<CaseStatus, CaseEvent> states) {
        states
            .withStates()
            .initial(CaseStatus.NEW)
            .states(EnumSet.allOf(CaseStatus.class))
            .end(CaseStatus.CLOSED_CLEARED)
            .end(CaseStatus.CLOSED_SAR_FILED)
            .end(CaseStatus.CLOSED_BLOCKED);
    }
    
    @Override
    public void configure(StateMachineTransitionConfigurer<CaseStatus, CaseEvent> transitions) {
        transitions
            .withExternal()
                .source(CaseStatus.NEW).target(CaseStatus.ASSIGNED)
                .event(CaseEvent.ASSIGN)
                .action(assignAction())
            .and()
            .withExternal()
                .source(CaseStatus.ASSIGNED).target(CaseStatus.IN_PROGRESS)
                .event(CaseEvent.START_INVESTIGATION)
                .guard(hasAssignedUser())
            .and()
            .withExternal()
                .source(CaseStatus.IN_PROGRESS).target(CaseStatus.PENDING_REVIEW)
                .event(CaseEvent.SUBMIT_FOR_REVIEW)
                .guard(hasRequiredFields())
            .and()
            .withExternal()
                .source(CaseStatus.IN_PROGRESS).target(CaseStatus.ESCALATED)
                .event(CaseEvent.ESCALATE)
                .action(escalateAction())
            .and()
            .withExternal()
                .source(CaseStatus.PENDING_REVIEW).target(CaseStatus.CLOSED_CLEARED)
                .event(CaseEvent.APPROVE_CLEARANCE)
            .and()
            .withExternal()
                .source(CaseStatus.PENDING_REVIEW).target(CaseStatus.CLOSED_SAR_FILED)
                .event(CaseEvent.APPROVE_SAR)
            .and()
            .withExternal()
                .source(CaseStatus.ESCALATED).target(CaseStatus.IN_PROGRESS)
                .event(CaseEvent.DE_ESCALATE);
    }
}
```

**Case Events Enum:**
```java
public enum CaseEvent {
    ASSIGN,
    START_INVESTIGATION,
    SUBMIT_FOR_REVIEW,
    ESCALATE,
    DE_ESCALATE,
    APPROVE_CLEARANCE,
    APPROVE_SAR,
    REJECT,
    REOPEN
}
```

#### Option B: Custom State Transition Service (Simpler)
If Spring State Machine is too complex, implement a custom service:

```java
@Service
public class CaseWorkflowService {
    
    private static final Map<CaseStatus, Set<CaseStatus>> ALLOWED_TRANSITIONS = Map.of(
        CaseStatus.NEW, Set.of(CaseStatus.ASSIGNED, CaseStatus.ESCALATED),
        CaseStatus.ASSIGNED, Set.of(CaseStatus.IN_PROGRESS, CaseStatus.ESCALATED, CaseStatus.NEW),
        CaseStatus.IN_PROGRESS, Set.of(CaseStatus.PENDING_REVIEW, CaseStatus.ESCALATED, CaseStatus.PENDING_INFO),
        CaseStatus.PENDING_REVIEW, Set.of(CaseStatus.CLOSED_CLEARED, CaseStatus.CLOSED_SAR_FILED, CaseStatus.IN_PROGRESS),
        CaseStatus.ESCALATED, Set.of(CaseStatus.IN_PROGRESS, CaseStatus.CLOSED_SAR_FILED),
        CaseStatus.PENDING_INFO, Set.of(CaseStatus.IN_PROGRESS, CaseStatus.ESCALATED)
    );
    
    public void transitionCase(ComplianceCase case, CaseStatus newStatus, Long userId) {
        validateTransition(case.getStatus(), newStatus);
        validatePermissions(case, newStatus, userId);
        
        CaseStatus oldStatus = case.getStatus();
        case.setStatus(newStatus);
        
        // Log state transition
        caseActivityService.logStateTransition(case.getId(), oldStatus, newStatus, userId);
        
        // Trigger state-specific actions
        handleStateEntry(case, newStatus);
    }
    
    private void validateTransition(CaseStatus from, CaseStatus to) {
        Set<CaseStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        if (allowed == null || !allowed.contains(to)) {
            throw new InvalidStateTransitionException(
                "Cannot transition from " + from + " to " + to
            );
        }
    }
}
```

### 1.2 Automatic Case Aging/SLA Tracking

**Implementation:**

```java
@Service
public class CaseSlaService {
    
    @Value("${case.sla.days.low:7}")
    private int slaDaysLow;
    
    @Value("${case.sla.days.medium:5}")
    private int slaDaysMedium;
    
    @Value("${case.sla.days.high:3}")
    private int slaDaysHigh;
    
    @Value("${case.sla.days.critical:1}")
    private int slaDaysCritical;
    
    @Autowired
    private BusinessDayCalculator businessDayCalculator;
    
    public void calculateSlaDeadline(ComplianceCase case) {
        int slaDays = getSlaDaysForPriority(case.getPriority());
        LocalDateTime deadline = businessDayCalculator.addBusinessDays(
            case.getCreatedAt(), 
            slaDays
        );
        case.setSlaDeadline(deadline);
    }
    
    public void updateCaseAging(ComplianceCase case) {
        LocalDateTime now = LocalDateTime.now();
        long daysOpen = ChronoUnit.DAYS.between(case.getCreatedAt(), now);
        case.setDaysOpen((int) daysOpen);
    }
    
    public CaseSlaStatus checkSlaStatus(ComplianceCase case) {
        if (case.getSlaDeadline() == null) {
            return CaseSlaStatus.NO_SLA;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(case.getSlaDeadline())) {
            return CaseSlaStatus.BREACHED;
        } else if (now.isAfter(case.getSlaDeadline().minusDays(1))) {
            return CaseSlaStatus.AT_RISK;
        }
        return CaseSlaStatus.ON_TRACK;
    }
    
    @Scheduled(cron = "0 0 1 * * *") // Daily at 1 AM
    public void updateAllCaseAging() {
        List<ComplianceCase> openCases = caseRepository.findByStatusIn(
            List.of(CaseStatus.NEW, CaseStatus.ASSIGNED, CaseStatus.IN_PROGRESS, 
                    CaseStatus.PENDING_REVIEW, CaseStatus.ESCALATED)
        );
        
        openCases.forEach(case -> {
            updateCaseAging(case);
            CaseSlaStatus slaStatus = checkSlaStatus(case);
            if (slaStatus == CaseSlaStatus.BREACHED || slaStatus == CaseSlaStatus.AT_RISK) {
                notifySlaBreach(case, slaStatus);
            }
        });
    }
}
```

**Business Day Calculator:**
```java
@Component
public class BusinessDayCalculator {
    
    private static final Set<DayOfWeek> WEEKEND = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    
    @Value("${business.holidays:}")
    private List<String> holidayDates; // Format: "2024-01-01,2024-12-25"
    
    public LocalDateTime addBusinessDays(LocalDateTime startDate, int businessDays) {
        LocalDateTime current = startDate;
        int daysAdded = 0;
        
        while (daysAdded < businessDays) {
            current = current.plusDays(1);
            if (isBusinessDay(current)) {
                daysAdded++;
            }
        }
        
        return current;
    }
    
    private boolean isBusinessDay(LocalDateTime date) {
        if (WEEKEND.contains(date.getDayOfWeek())) {
            return false;
        }
        
        String dateStr = date.toLocalDate().toString();
        return !holidayDates.contains(dateStr);
    }
}
```

---

## 2. Case Assignment & Distribution

### 2.1 Automatic Case Assignment Based on Workload

**Implementation Strategies:**

#### Strategy 1: Round-Robin Assignment
```java
@Service
public class CaseAssignmentService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ComplianceCaseRepository caseRepository;
    
    public User assignCaseRoundRobin(ComplianceCase case, UserRole requiredRole) {
        List<User> availableUsers = userRepository.findByRoleAndActive(requiredRole, true);
        
        if (availableUsers.isEmpty()) {
            throw new NoAvailableUserException("No users available for role: " + requiredRole);
        }
        
        // Get user with least recent assignment
        User assignedUser = availableUsers.stream()
            .min(Comparator.comparing(this::getLastAssignmentTime))
            .orElse(availableUsers.get(0));
        
        assignCaseToUser(case, assignedUser);
        return assignedUser;
    }
    
    private LocalDateTime getLastAssignmentTime(User user) {
        return caseRepository.findLastAssignmentTimeByUser(user.getId())
            .orElse(LocalDateTime.MIN);
    }
}
```

#### Strategy 2: Workload-Based Assignment (Recommended)
```java
@Service
public class WorkloadBasedAssignmentService {
    
    @Value("${case.assignment.max-cases-per-user:20}")
    private int maxCasesPerUser;
    
    public User assignCaseByWorkload(ComplianceCase case, UserRole requiredRole) {
        List<User> availableUsers = userRepository.findByRoleAndActive(requiredRole, true);
        
        // Calculate workload for each user
        Map<User, Integer> workloads = availableUsers.stream()
            .collect(Collectors.toMap(
                user -> user,
                user -> getCurrentWorkload(user)
            ));
        
        // Filter users with capacity
        List<User> usersWithCapacity = workloads.entrySet().stream()
            .filter(entry -> entry.getValue() < maxCasesPerUser)
            .sorted(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        if (usersWithCapacity.isEmpty()) {
            // All users at capacity - assign to user with lowest workload
            User assignedUser = workloads.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new NoAvailableUserException("No users available"));
            assignCaseToUser(case, assignedUser);
            return assignedUser;
        }
        
        // Assign to user with lowest workload
        User assignedUser = usersWithCapacity.get(0);
        assignCaseToUser(case, assignedUser);
        return assignedUser;
    }
    
    private int getCurrentWorkload(User user) {
        return caseRepository.countByAssignedToAndStatusIn(
            user,
            List.of(CaseStatus.ASSIGNED, CaseStatus.IN_PROGRESS, CaseStatus.PENDING_REVIEW)
        );
    }
}
```

#### Strategy 3: Skill-Based Assignment
```java
@Service
public class SkillBasedAssignmentService {
    
    public User assignCaseBySkills(ComplianceCase case, Set<String> requiredSkills) {
        List<User> availableUsers = userRepository.findByRoleAndActive(UserRole.INVESTIGATOR, true);
        
        // Score users based on skill match and workload
        Map<User, Double> userScores = availableUsers.stream()
            .collect(Collectors.toMap(
                user -> user,
                user -> calculateAssignmentScore(user, requiredSkills, case.getPriority())
            ));
        
        User assignedUser = userScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElseThrow(() -> new NoAvailableUserException("No users available"));
        
        assignCaseToUser(case, assignedUser);
        return assignedUser;
    }
    
    private double calculateAssignmentScore(User user, Set<String> requiredSkills, CasePriority priority) {
        double skillScore = calculateSkillMatch(user.getSkills(), requiredSkills);
        double workloadScore = 1.0 / (1.0 + getCurrentWorkload(user));
        double priorityScore = priority == CasePriority.CRITICAL ? 1.5 : 1.0;
        
        return skillScore * workloadScore * priorityScore;
    }
}
```

### 2.2 Case Queue Management

**Implementation:**

```java
@Entity
@Table(name = "case_queues")
public class CaseQueue {
    @Id
    @GeneratedValue
    private Long id;
    
    private String queueName; // e.g., "HIGH_PRIORITY", "SANCTIONS", "PEP"
    private UserRole targetRole;
    private CasePriority minPriority;
    private Integer maxQueueSize;
    private Boolean autoAssign;
    
    @OneToMany(mappedBy = "queue")
    private List<ComplianceCase> queuedCases;
}

@Service
public class CaseQueueService {
    
    public void addCaseToQueue(ComplianceCase case, String queueName) {
        CaseQueue queue = queueRepository.findByName(queueName)
            .orElseThrow(() -> new QueueNotFoundException(queueName));
        
        validateCaseEligibility(case, queue);
        
        case.setQueue(queue);
        case.setStatus(CaseStatus.NEW);
        caseRepository.save(case);
        
        if (queue.getAutoAssign()) {
            autoAssignFromQueue(queue);
        }
    }
    
    @Scheduled(fixedDelay = 60000) // Every minute
    public void processAutoAssignQueues() {
        List<CaseQueue> autoAssignQueues = queueRepository.findByAutoAssignTrue();
        autoAssignQueues.forEach(this::autoAssignFromQueue);
    }
    
    private void autoAssignFromQueue(CaseQueue queue) {
        List<ComplianceCase> queuedCases = caseRepository.findByQueueAndStatus(
            queue, CaseStatus.NEW
        );
        
        queuedCases.forEach(case -> {
            try {
                User assignedUser = assignmentService.assignCaseByWorkload(
                    case, 
                    queue.getTargetRole()
                );
                case.setStatus(CaseStatus.ASSIGNED);
                caseRepository.save(case);
            } catch (NoAvailableUserException e) {
                log.warn("No user available for case {} in queue {}", 
                    case.getId(), queue.getQueueName());
            }
        });
    }
}
```

---

## 3. Investigation Tools

### 3.1 Timeline View of Related Transactions

**Implementation:**

```java
@Entity
@Table(name = "case_transactions")
public class CaseTransaction {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "case_id")
    private ComplianceCase complianceCase;
    
    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private TransactionEntity transaction;
    
    private String relationshipType; // PRIMARY, RELATED, SUSPICIOUS_PATTERN
    private LocalDateTime addedAt;
    private Long addedBy;
}

@Service
public class CaseTimelineService {
    
    public CaseTimelineDTO buildTimeline(Long caseId) {
        ComplianceCase case = caseRepository.findById(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));
        
        List<TimelineEvent> events = new ArrayList<>();
        
        // Case creation
        events.add(TimelineEvent.builder()
            .timestamp(case.getCreatedAt())
            .type("CASE_CREATED")
            .description("Case created")
            .build());
        
        // Case assignments
        if (case.getAssignedAt() != null) {
            events.add(TimelineEvent.builder()
                .timestamp(case.getAssignedAt())
                .type("CASE_ASSIGNED")
                .description("Assigned to " + case.getAssignedTo().getUsername())
                .build());
        }
        
        // Related transactions
        List<CaseTransaction> caseTransactions = caseTransactionRepository.findByComplianceCase(case);
        caseTransactions.forEach(ct -> {
            events.add(TimelineEvent.builder()
                .timestamp(ct.getTransaction().getTimestamp())
                .type("TRANSACTION")
                .description("Transaction: " + ct.getTransaction().getTransactionId())
                .data(ct.getTransaction())
                .build());
        });
        
        // Case notes
        case.getNotes().forEach(note -> {
            events.add(TimelineEvent.builder()
                .timestamp(note.getCreatedAt())
                .type("NOTE")
                .description("Note added by " + note.getAuthor().getUsername())
                .data(note)
                .build());
        });
        
        // Escalations
        if (case.getEscalatedAt() != null) {
            events.add(TimelineEvent.builder()
                .timestamp(case.getEscalatedAt())
                .type("ESCALATION")
                .description("Escalated: " + case.getEscalationReason())
                .build());
        }
        
        // Sort by timestamp
        events.sort(Comparator.comparing(TimelineEvent::getTimestamp));
        
        return CaseTimelineDTO.builder()
            .caseId(caseId)
            .events(events)
            .build();
    }
}
```

### 3.2 Network/Relationship Visualization

**Implementation:**

```java
@Service
public class CaseNetworkService {
    
    public NetworkGraphDTO buildNetworkGraph(Long caseId, int depth) {
        ComplianceCase rootCase = caseRepository.findById(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));
        
        NetworkGraphDTO graph = new NetworkGraphDTO();
        
        // Add root case as node
        graph.addNode(createCaseNode(rootCase));
        
        // Find related cases
        Set<ComplianceCase> relatedCases = findRelatedCases(rootCase, depth);
        relatedCases.forEach(relatedCase -> {
            graph.addNode(createCaseNode(relatedCase));
            graph.addEdge(createCaseEdge(rootCase, relatedCase));
        });
        
        // Find related transactions
        List<TransactionEntity> transactions = findRelatedTransactions(rootCase);
        transactions.forEach(tx -> {
            graph.addNode(createTransactionNode(tx));
            graph.addEdge(createCaseTransactionEdge(rootCase, tx));
        });
        
        // Find related entities (merchants, customers)
        Set<EntityNode> entities = findRelatedEntities(rootCase);
        entities.forEach(entity -> {
            graph.addNode(entity);
            graph.addEdge(createCaseEntityEdge(rootCase, entity));
        });
        
        return graph;
    }
    
    private Set<ComplianceCase> findRelatedCases(ComplianceCase case, int depth) {
        Set<ComplianceCase> related = new HashSet<>();
        Queue<ComplianceCase> queue = new LinkedList<>();
        queue.add(case);
        int currentDepth = 0;
        
        while (!queue.isEmpty() && currentDepth < depth) {
            ComplianceCase current = queue.poll();
            Set<ComplianceCase> directRelated = current.getRelatedCases();
            related.addAll(directRelated);
            queue.addAll(directRelated);
            currentDepth++;
        }
        
        return related;
    }
}
```

### 3.3 Evidence Attachment System

**Implementation:**

```java
@Entity
@Table(name = "case_evidence")
public class CaseEvidence {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "case_id")
    private ComplianceCase complianceCase;
    
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storagePath; // S3 path or local path
    private String evidenceType; // DOCUMENT, SCREENSHOT, EMAIL, AUDIO, VIDEO
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
    
    private LocalDateTime uploadedAt;
    private String checksum; // For integrity verification
}

@Service
public class CaseEvidenceService {
    
    @Value("${evidence.storage.type:local}") // local, s3
    private String storageType;
    
    @Value("${evidence.storage.path:/var/aml/evidence}")
    private String storagePath;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    public CaseEvidence attachEvidence(Long caseId, MultipartFile file, 
                                      String evidenceType, String description, 
                                      Long userId) {
        ComplianceCase case = caseRepository.findById(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));
        
        // Validate file
        validateFile(file);
        
        // Generate unique filename
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
        
        // Store file
        String storagePath = fileStorageService.store(file, uniqueFileName);
        
        // Calculate checksum
        String checksum = calculateChecksum(file);
        
        // Create evidence record
        CaseEvidence evidence = CaseEvidence.builder()
            .complianceCase(case)
            .fileName(file.getOriginalFilename())
            .fileType(file.getContentType())
            .fileSize(file.getSize())
            .storagePath(storagePath)
            .evidenceType(evidenceType)
            .description(description)
            .uploadedBy(userRepository.findById(userId).orElseThrow())
            .uploadedAt(LocalDateTime.now())
            .checksum(checksum)
            .build();
        
        return evidenceRepository.save(evidence);
    }
    
    public byte[] downloadEvidence(Long evidenceId) {
        CaseEvidence evidence = evidenceRepository.findById(evidenceId)
            .orElseThrow(() -> new EvidenceNotFoundException(evidenceId));
        
        return fileStorageService.retrieve(evidence.getStoragePath());
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidFileException("File is empty");
        }
        
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new InvalidFileException("File size exceeds maximum allowed");
        }
        
        Set<String> allowedTypes = Set.of(
            "application/pdf",
            "image/jpeg", "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        
        if (!allowedTypes.contains(file.getContentType())) {
            throw new InvalidFileException("File type not allowed");
        }
    }
}
```

### 3.4 Investigation Notes with Threading

**Implementation:**

```java
@Entity
@Table(name = "case_notes")
public class CaseNote {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "case_id")
    private ComplianceCase complianceCase;
    
    @ManyToOne
    @JoinColumn(name = "parent_note_id")
    private CaseNote parentNote; // For threading
    
    @OneToMany(mappedBy = "parentNote")
    private List<CaseNote> replies;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    private Boolean internal = true; // Internal vs external note
    
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // @Mention tracking
    @ElementCollection
    @CollectionTable(name = "case_note_mentions")
    private Set<Long> mentionedUserIds;
}

@Service
public class CaseNoteService {
    
    @Autowired
    private NotificationService notificationService;
    
    public CaseNote addNote(Long caseId, String content, Long authorId, 
                           Long parentNoteId, Boolean internal) {
        ComplianceCase case = caseRepository.findById(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));
        
        CaseNote parentNote = null;
        if (parentNoteId != null) {
            parentNote = noteRepository.findById(parentNoteId)
                .orElseThrow(() -> new NoteNotFoundException(parentNoteId));
        }
        
        // Extract @mentions
        Set<Long> mentionedUserIds = extractMentions(content);
        
        CaseNote note = CaseNote.builder()
            .complianceCase(case)
            .parentNote(parentNote)
            .content(content)
            .internal(internal)
            .author(userRepository.findById(authorId).orElseThrow())
            .createdAt(LocalDateTime.now())
            .mentionedUserIds(mentionedUserIds)
            .build();
        
        note = noteRepository.save(note);
        
        // Notify mentioned users
        mentionedUserIds.forEach(userId -> {
            notificationService.notifyUserMentioned(userId, caseId, note.getId());
        });
        
        // Log activity
        caseActivityService.logNoteAdded(caseId, note.getId(), authorId);
        
        return note;
    }
    
    private Set<Long> extractMentions(String content) {
        Pattern mentionPattern = Pattern.compile("@(\\w+)");
        Matcher matcher = mentionPattern.matcher(content);
        Set<Long> userIds = new HashSet<>();
        
        while (matcher.find()) {
            String username = matcher.group(1);
            userRepository.findByUsername(username)
                .ifPresent(user -> userIds.add(user.getId()));
        }
        
        return userIds;
    }
    
    public List<CaseNote> getNoteThread(Long caseId) {
        ComplianceCase case = caseRepository.findById(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));
        
        // Get all notes for case, organized by thread
        List<CaseNote> rootNotes = noteRepository.findByComplianceCaseAndParentNoteIsNull(case);
        
        return rootNotes.stream()
            .map(this::buildNoteThread)
            .flatMap(List::stream)
            .sorted(Comparator.comparing(CaseNote::getCreatedAt))
            .collect(Collectors.toList());
    }
    
    private List<CaseNote> buildNoteThread(CaseNote rootNote) {
        List<CaseNote> thread = new ArrayList<>();
        thread.add(rootNote);
        
        List<CaseNote> replies = noteRepository.findByParentNote(rootNote);
        replies.forEach(reply -> {
            thread.addAll(buildNoteThread(reply));
        });
        
        return thread;
    }
}
```

---

## 4. Collaboration Features

### 4.1 Case Activity Feed

**Implementation:**

```java
@Entity
@Table(name = "case_activities")
public class CaseActivity {
    @Id
    @GeneratedValue
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "case_id")
    private ComplianceCase complianceCase;
    
    @Enumerated(EnumType.STRING)
    private ActivityType activityType;
    
    private String description;
    private String details; // JSON for structured data
    
    @ManyToOne
    @JoinColumn(name = "performed_by")
    private User performedBy;
    
    private LocalDateTime performedAt;
    
    // Related entities
    private Long relatedEntityId; // Note ID, Evidence ID, etc.
    private String relatedEntityType;
}

public enum ActivityType {
    CASE_CREATED,
    CASE_ASSIGNED,
    CASE_STATUS_CHANGED,
    NOTE_ADDED,
    EVIDENCE_ATTACHED,
    CASE_ESCALATED,
    CASE_REASSIGNED,
    SAR_CREATED,
    CASE_CLOSED
}

@Service
public class CaseActivityService {
    
    public void logActivity(Long caseId, ActivityType activityType, 
                           String description, Long userId, 
                           Map<String, Object> details) {
        CaseActivity activity = CaseActivity.builder()
            .complianceCase(caseRepository.getReferenceById(caseId))
            .activityType(activityType)
            .description(description)
            .details(objectMapper.writeValueAsString(details))
            .performedBy(userRepository.getReferenceById(userId))
            .performedAt(LocalDateTime.now())
            .build();
        
        activityRepository.save(activity);
    }
    
    public List<CaseActivityDTO> getActivityFeed(Long caseId, int limit) {
        List<CaseActivity> activities = activityRepository
            .findByComplianceCaseIdOrderByPerformedAtDesc(caseId, 
                PageRequest.of(0, limit));
        
        return activities.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
}
```

### 4.2 @Mention System

**Implementation:**

```java
@Service
public class MentionService {
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;
    
    public void processMentions(String content, Long caseId, Long authorId) {
        Set<Long> mentionedUserIds = extractMentions(content);
        
        mentionedUserIds.forEach(userId -> {
            // Create mention record
            CaseMention mention = CaseMention.builder()
                .caseId(caseId)
                .mentionedUserId(userId)
                .mentionedByUserId(authorId)
                .mentionedAt(LocalDateTime.now())
                .read(false)
                .build();
            
            mentionRepository.save(mention);
            
            // Send notification
            notificationService.notifyUserMentioned(userId, caseId);
        });
    }
    
    public List<CaseMention> getUnreadMentions(Long userId) {
        return mentionRepository.findByMentionedUserIdAndReadFalse(userId);
    }
    
    public void markMentionAsRead(Long mentionId) {
        CaseMention mention = mentionRepository.findById(mentionId)
            .orElseThrow(() -> new MentionNotFoundException(mentionId));
        mention.setRead(true);
        mentionRepository.save(mention);
    }
}
```

---

## 5. Case Escalation

### 5.1 Escalation Workflow

**Implementation:**

```java
@Entity
@Table(name = "escalation_rules")
public class EscalationRule {
    @Id
    @GeneratedValue
    private Long id;
    
    private String ruleName;
    private Boolean enabled;
    
    // Conditions
    private CasePriority minPriority;
    private Double minRiskScore;
    private BigDecimal minAmount;
    private Integer daysOpen;
    
    // Escalation target
    private UserRole escalateToRole;
    private Long escalateToUserId; // Specific user override
    
    // Escalation reason template
    private String reasonTemplate;
}

@Service
public class CaseEscalationService {
    
    @Autowired
    private EscalationRuleRepository escalationRuleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public void escalateCase(Long caseId, String reason, Long escalatedBy) {
        ComplianceCase case = caseRepository.findById(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));
        
        // Determine escalation target
        User escalationTarget = determineEscalationTarget(case);
        
        // Update case
        case.setEscalated(true);
        case.setEscalatedTo(escalationTarget.getId());
        case.setEscalationReason(reason);
        case.setEscalatedAt(LocalDateTime.now());
        case.setStatus(CaseStatus.ESCALATED);
        
        caseRepository.save(case);
        
        // Log activity
        caseActivityService.logEscalation(caseId, escalationTarget.getId(), reason, escalatedBy);
        
        // Notify escalation target
        notificationService.notifyCaseEscalated(escalationTarget.getId(), caseId);
    }
    
    public void checkAutomaticEscalation(ComplianceCase case) {
        List<EscalationRule> rules = escalationRuleRepository.findByEnabledTrue();
        
        for (EscalationRule rule : rules) {
            if (matchesEscalationRule(case, rule)) {
                String reason = buildEscalationReason(case, rule);
                escalateCase(case.getId(), reason, null); // System escalation
                break; // Only escalate once
            }
        }
    }
    
    private boolean matchesEscalationRule(ComplianceCase case, EscalationRule rule) {
        // Check priority
        if (rule.getMinPriority() != null && 
            case.getPriority().ordinal() < rule.getMinPriority().ordinal()) {
            return false;
        }
        
        // Check risk score (if available)
        if (rule.getMinRiskScore() != null) {
            Double riskScore = getCaseRiskScore(case);
            if (riskScore == null || riskScore < rule.getMinRiskScore()) {
                return false;
            }
        }
        
        // Check days open
        if (rule.getDaysOpen() != null) {
            if (case.getDaysOpen() == null || case.getDaysOpen() < rule.getDaysOpen()) {
                return false;
            }
        }
        
        return true;
    }
    
    private User determineEscalationTarget(ComplianceCase case) {
        // Check if case already has escalation rule match
        EscalationRule matchingRule = findMatchingEscalationRule(case);
        
        if (matchingRule != null && matchingRule.getEscalateToUserId() != null) {
            return userRepository.findById(matchingRule.getEscalateToUserId())
                .orElseThrow();
        }
        
        // Default escalation hierarchy
        UserRole targetRole = determineEscalationRole(case);
        List<User> availableUsers = userRepository.findByRoleAndActive(targetRole, true);
        
        if (availableUsers.isEmpty()) {
            // Escalate to next level
            targetRole = getNextEscalationRole(targetRole);
            availableUsers = userRepository.findByRoleAndActive(targetRole, true);
        }
        
        return availableUsers.stream()
            .min(Comparator.comparing(this::getCurrentWorkload))
            .orElseThrow(() -> new NoAvailableUserException("No escalation target available"));
    }
    
    private UserRole determineEscalationRole(ComplianceCase case) {
        // Escalation hierarchy: ANALYST -> COMPLIANCE_OFFICER -> MLRO
        User currentAssignee = case.getAssignedTo();
        
        if (currentAssignee == null) {
            return UserRole.COMPLIANCE_OFFICER;
        }
        
        switch (currentAssignee.getRole()) {
            case ANALYST:
                return UserRole.COMPLIANCE_OFFICER;
            case COMPLIANCE_OFFICER:
                return UserRole.MLRO;
            case COMPLIANCE_OFFICER:
                return UserRole.MLRO; // Already at highest level
            default:
                return UserRole.COMPLIANCE_OFFICER;
        }
    }
}
```

### 5.2 Automatic Escalation Based on Risk Score or Amount

**Implementation:**

```java
@Service
public class AutomaticEscalationService {
    
    @Value("${escalation.auto.enabled:true}")
    private Boolean autoEscalationEnabled;
    
    @Value("${escalation.auto.risk-score-threshold:0.8}")
    private Double riskScoreThreshold;
    
    @Value("${escalation.auto.amount-threshold:100000}")
    private BigDecimal amountThreshold;
    
    @EventListener
    public void onCaseCreated(CaseCreatedEvent event) {
        if (!autoEscalationEnabled) {
            return;
        }
        
        ComplianceCase case = caseRepository.findById(event.getCaseId())
            .orElseThrow();
        
        checkAndEscalate(case);
    }
    
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void checkPendingEscalations() {
        if (!autoEscalationEnabled) {
            return;
        }
        
        List<ComplianceCase> casesToCheck = caseRepository.findByEscalatedFalseAndStatusIn(
            List.of(CaseStatus.NEW, CaseStatus.ASSIGNED, CaseStatus.IN_PROGRESS)
        );
        
        casesToCheck.forEach(this::checkAndEscalate);
    }
    
    private void checkAndEscalate(ComplianceCase case) {
        boolean shouldEscalate = false;
        String reason = null;
        
        // Check risk score
        Double riskScore = getCaseRiskScore(case);
        if (riskScore != null && riskScore >= riskScoreThreshold) {
            shouldEscalate = true;
            reason = String.format("Automatic escalation: Risk score %.2f exceeds threshold %.2f", 
                riskScore, riskScoreThreshold);
        }
        
        // Check transaction amount
        BigDecimal totalAmount = getCaseTotalAmount(case);
        if (totalAmount != null && totalAmount.compareTo(amountThreshold) >= 0) {
            shouldEscalate = true;
            reason = String.format("Automatic escalation: Total amount %s exceeds threshold %s", 
                totalAmount, amountThreshold);
        }
        
        // Check priority
        if (case.getPriority() == CasePriority.CRITICAL) {
            shouldEscalate = true;
            reason = "Automatic escalation: Case marked as CRITICAL priority";
        }
        
        if (shouldEscalate) {
            escalationService.escalateCase(case.getId(), reason, null);
        }
    }
    
    private Double getCaseRiskScore(ComplianceCase case) {
        // Get risk score from related transactions or case itself
        // Implementation depends on your risk scoring system
        return null; // Placeholder
    }
    
    private BigDecimal getCaseTotalAmount(ComplianceCase case) {
        // Sum of all related transaction amounts
        List<TransactionEntity> transactions = getCaseTransactions(case);
        return transactions.stream()
            .map(TransactionEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

---

## Implementation Priority & Dependencies

### Phase 1: Core Workflow (Week 1-2)
1. ✅ Case Status enum (already exists)
2. ✅ Case Priority enum (already exists)
3. Implement state transition validation
4. Implement SLA deadline calculation
5. Implement case aging tracking

### Phase 2: Assignment & Distribution (Week 2-3)
1. Implement workload-based assignment
2. Implement case queue management
3. Implement case reassignment workflow

### Phase 3: Investigation Tools (Week 3-4)
1. Implement timeline view
2. Implement evidence attachment system
3. Implement investigation notes with threading
4. Implement @mention system

### Phase 4: Collaboration & Escalation (Week 4-5)
1. Implement activity feed
2. Implement escalation workflow
3. Implement automatic escalation rules

### Phase 5: Advanced Features (Week 5-6)
1. Implement network/relationship visualization
2. Implement customer profile aggregation view
3. Implement case linking

---

## Database Schema Updates Required

```sql
-- Case Queues
CREATE TABLE case_queues (
    id BIGSERIAL PRIMARY KEY,
    queue_name VARCHAR(100) NOT NULL UNIQUE,
    target_role VARCHAR(50),
    min_priority VARCHAR(20),
    max_queue_size INT,
    auto_assign BOOLEAN DEFAULT FALSE
);

-- Case Activities
CREATE TABLE case_activities (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES compliance_cases(id),
    activity_type VARCHAR(50) NOT NULL,
    description TEXT,
    details JSONB,
    performed_by BIGINT REFERENCES platform_users(id),
    performed_at TIMESTAMP NOT NULL,
    related_entity_id BIGINT,
    related_entity_type VARCHAR(50)
);

-- Case Mentions
CREATE TABLE case_mentions (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES compliance_cases(id),
    mentioned_user_id BIGINT NOT NULL REFERENCES platform_users(id),
    mentioned_by_user_id BIGINT REFERENCES platform_users(id),
    mentioned_at TIMESTAMP NOT NULL,
    read BOOLEAN DEFAULT FALSE
);

-- Escalation Rules
CREATE TABLE escalation_rules (
    id BIGSERIAL PRIMARY KEY,
    rule_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    min_priority VARCHAR(20),
    min_risk_score DOUBLE PRECISION,
    min_amount DECIMAL(19,2),
    days_open INT,
    escalate_to_role VARCHAR(50),
    escalate_to_user_id BIGINT REFERENCES platform_users(id),
    reason_template TEXT
);

-- Case Transactions (for timeline)
CREATE TABLE case_transactions (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES compliance_cases(id),
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    relationship_type VARCHAR(50),
    added_at TIMESTAMP NOT NULL,
    added_by BIGINT REFERENCES platform_users(id)
);

-- Indexes
CREATE INDEX idx_case_activities_case ON case_activities(case_id, performed_at DESC);
CREATE INDEX idx_case_mentions_user ON case_mentions(mentioned_user_id, read);
CREATE INDEX idx_case_transactions_case ON case_transactions(case_id);
```

---

## Testing Recommendations

1. **Unit Tests:**
   - State transition validation
   - SLA calculation logic
   - Assignment algorithm correctness
   - Escalation rule matching

2. **Integration Tests:**
   - End-to-end case workflow
   - Assignment service with database
   - Evidence upload and retrieval
   - Activity feed generation

3. **Performance Tests:**
   - Case assignment under load
   - Timeline generation for cases with many transactions
   - Activity feed pagination

---

## References & Best Practices

1. **Workflow State Machines:**
   - Spring State Machine documentation
   - State pattern in enterprise applications

2. **Load Balancing:**
   - Round-robin vs. least-connections algorithms
   - Workload distribution strategies

3. **Activity Feeds:**
   - Event sourcing patterns
   - Activity stream specifications

4. **File Storage:**
   - AWS S3 best practices
   - File integrity verification (checksums)

5. **Escalation Patterns:**
   - Business rule engines
   - Decision tree implementations

