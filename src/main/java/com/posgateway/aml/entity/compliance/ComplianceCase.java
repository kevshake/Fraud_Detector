package com.posgateway.aml.entity.compliance;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.model.CasePriority;
import com.posgateway.aml.model.CaseStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Compliance Case Entity
 * Represents an investigation case for suspicious activity
 */
@Entity
@Table(name = "compliance_cases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String caseReference; // e.g., CASE-2023-0001

    @Column(name = "merchant_id")
    private String merchantId; // optional merchant association for filtering

    @Column(columnDefinition = "TEXT")
    private String description;
    
    // NEW: Case workflow
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CaseStatus status = CaseStatus.NEW;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;
    
    private LocalDateTime slaDeadline; // NEW: SLA tracking
    
    private Integer daysOpen; // NEW: Case aging
    
    // NEW: Assignment tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;
    
    @Column(name = "assigned_by_user_id")
    private Long assignedBy;
    
    private LocalDateTime assignedAt;
    
    // NEW: Escalation tracking
    @Builder.Default
    private Boolean escalated = false;
    
    @Column(name = "escalated_to_user_id")
    private Long escalatedTo;
    
    private String escalationReason;
    
    private LocalDateTime escalatedAt;
    
    // NEW: Case relationships
    @ManyToMany
    @JoinTable(
        name = "case_relationships",
        joinColumns = @JoinColumn(name = "case_id"),
        inverseJoinColumns = @JoinColumn(name = "related_case_id")
    )
    private Set<ComplianceCase> relatedCases;
    
    // NEW: Evidence and documentation
    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaseEvidence> evidence;
    
    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CaseNote> notes;
    
    // NEW: Decision tracking
    private String resolution; // CLEARED, SAR_FILED, BLOCKED, etc.
    
    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;
    
    private Long resolvedBy;
    
    private LocalDateTime resolvedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = CaseStatus.NEW;
        if (priority == null) priority = CasePriority.MEDIUM;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
