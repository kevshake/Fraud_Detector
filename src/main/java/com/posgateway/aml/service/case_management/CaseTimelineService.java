package com.posgateway.aml.service.case_management;

import com.posgateway.aml.entity.TransactionEntity;
import com.posgateway.aml.entity.compliance.CaseActivity;
import com.posgateway.aml.entity.compliance.CaseNote;
import com.posgateway.aml.entity.compliance.CaseTransaction;
import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.model.ActivityType;
import com.posgateway.aml.repository.CaseActivityRepository;
import com.posgateway.aml.repository.CaseTransactionRepository;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Case Timeline Service
 * Builds chronological timeline of events for a case
 */
@Service
public class CaseTimelineService {

    private static final Logger logger = LoggerFactory.getLogger(CaseTimelineService.class);

    private final ComplianceCaseRepository caseRepository;
    private final CaseActivityRepository activityRepository;
    private final CaseTransactionRepository caseTransactionRepository;

    @Autowired
    public CaseTimelineService(ComplianceCaseRepository caseRepository,
                              CaseActivityRepository activityRepository,
                              CaseTransactionRepository caseTransactionRepository) {
        this.caseRepository = caseRepository;
        this.activityRepository = activityRepository;
        this.caseTransactionRepository = caseTransactionRepository;
    }

    /**
     * Build timeline for a case
     */
    public CaseTimelineDTO buildTimeline(Long caseId) {
        ComplianceCase complianceCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));

        List<TimelineEvent> events = new ArrayList<>();

        // Case creation
        events.add(TimelineEvent.builder()
                .timestamp(complianceCase.getCreatedAt())
                .type("CASE_CREATED")
                .description("Case created")
                .build());

        // Case assignments
        if (complianceCase.getAssignedAt() != null) {
            events.add(TimelineEvent.builder()
                    .timestamp(complianceCase.getAssignedAt())
                    .type("CASE_ASSIGNED")
                    .description("Assigned to " + 
                            (complianceCase.getAssignedTo() != null ? 
                                    complianceCase.getAssignedTo().getUsername() : "Unknown"))
                    .build());
        }

        // Related transactions
        List<CaseTransaction> caseTransactions = caseTransactionRepository.findByComplianceCase(complianceCase);
        caseTransactions.forEach(ct -> {
            if (ct.getTransaction() != null && ct.getTransaction().getTxnTs() != null) {
                events.add(TimelineEvent.builder()
                        .timestamp(ct.getTransaction().getTxnTs())
                        .type("TRANSACTION")
                        .description("Transaction: " + ct.getTransaction().getTxnId())
                        .data(ct.getTransaction())
                        .build());
            }
        });

        // Case notes
        if (complianceCase.getNotes() != null) {
            complianceCase.getNotes().forEach(note -> {
                events.add(TimelineEvent.builder()
                        .timestamp(note.getCreatedAt())
                        .type("NOTE")
                        .description("Note added by " + note.getAuthor().getUsername())
                        .data(note)
                        .build());
            });
        }

        // Escalations
        if (complianceCase.getEscalatedAt() != null) {
            events.add(TimelineEvent.builder()
                    .timestamp(complianceCase.getEscalatedAt())
                    .type("ESCALATION")
                    .description("Escalated: " + complianceCase.getEscalationReason())
                    .build());
        }

        // Case activities
        List<CaseActivity> activities = activityRepository.findByComplianceCaseIdOrderByPerformedAtDesc(
                caseId, 
                org.springframework.data.domain.PageRequest.of(0, 100)
        ).getContent();
        
        activities.forEach(activity -> {
            events.add(TimelineEvent.builder()
                    .timestamp(activity.getPerformedAt())
                    .type(activity.getActivityType().name())
                    .description(activity.getDescription())
                    .data(activity)
                    .build());
        });

        // Sort by timestamp
        events.sort(Comparator.comparing(TimelineEvent::getTimestamp));

        return CaseTimelineDTO.builder()
                .caseId(caseId)
                .caseReference(complianceCase.getCaseReference())
                .events(events)
                .build();
    }

    /**
     * Timeline Event DTO
     */
    public static class TimelineEvent {
        private LocalDateTime timestamp;
        private String type;
        private String description;
        private Object data;

        public static TimelineEventBuilder builder() {
            return new TimelineEventBuilder();
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getType() {
            return type;
        }

        public String getDescription() {
            return description;
        }

        public Object getData() {
            return data;
        }

        public static class TimelineEventBuilder {
            private LocalDateTime timestamp;
            private String type;
            private String description;
            private Object data;

            public TimelineEventBuilder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public TimelineEventBuilder type(String type) {
                this.type = type;
                return this;
            }

            public TimelineEventBuilder description(String description) {
                this.description = description;
                return this;
            }

            public TimelineEventBuilder data(Object data) {
                this.data = data;
                return this;
            }

            public TimelineEvent build() {
                TimelineEvent event = new TimelineEvent();
                event.timestamp = this.timestamp;
                event.type = this.type;
                event.description = this.description;
                event.data = this.data;
                return event;
            }
        }
    }

    /**
     * Case Timeline DTO
     */
    public static class CaseTimelineDTO {
        private Long caseId;
        private String caseReference;
        private List<TimelineEvent> events;

        public static CaseTimelineDTOBuilder builder() {
            return new CaseTimelineDTOBuilder();
        }

        public Long getCaseId() {
            return caseId;
        }

        public String getCaseReference() {
            return caseReference;
        }

        public List<TimelineEvent> getEvents() {
            return events;
        }

        public static class CaseTimelineDTOBuilder {
            private Long caseId;
            private String caseReference;
            private List<TimelineEvent> events;

            public CaseTimelineDTOBuilder caseId(Long caseId) {
                this.caseId = caseId;
                return this;
            }

            public CaseTimelineDTOBuilder caseReference(String caseReference) {
                this.caseReference = caseReference;
                return this;
            }

            public CaseTimelineDTOBuilder events(List<TimelineEvent> events) {
                this.events = events;
                return this;
            }

            public CaseTimelineDTO build() {
                CaseTimelineDTO dto = new CaseTimelineDTO();
                dto.caseId = this.caseId;
                dto.caseReference = this.caseReference;
                dto.events = this.events;
                return dto;
            }
        }
    }
}

