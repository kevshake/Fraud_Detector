package com.posgateway.aml.entity.compliance;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.TransactionEntity;
import com.posgateway.aml.model.SarStatus;
import com.posgateway.aml.model.SarType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Suspicious Activity Report (SAR) Entity
 * Represents a formal report to be filed with regulatory authorities
 */
@Entity
@Table(name = "suspicious_activity_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousActivityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sarReference; // e.g., SAR-2023-0001

    // Workflow Tracking
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SarStatus status = SarStatus.DRAFT;

    // Approval Workflow
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy; // Analyst

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy; // Compliance Officer
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy; // MLRO
    
    private LocalDateTime approvedAt;

    // Filing Tracking
    private String filingReferenceNumber; // From regulator
    private LocalDateTime filedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filed_by_user_id")
    private User filedBy;
    
    @Column(columnDefinition = "TEXT")
    private String filingReceipt; // Path to receipt or receipt content

    // Regulatory Requirements
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SarType sarType = SarType.INITIAL;

    @Column(nullable = false)
    private String jurisdiction; // US, UK, EU, etc.
    
    private LocalDateTime filingDeadline;

    // SAR Content
    @Column(nullable = false)
    private String suspiciousActivityType; // Structuring, Terrorist Financing, etc.
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String narrative; // Detailed explanation
    
    private BigDecimal totalSuspiciousAmount;

    // Related Entities
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private ComplianceCase complianceCase;

    @ManyToMany
    @JoinTable(
        name = "sar_transactions",
        joinColumns = @JoinColumn(name = "sar_id"),
        inverseJoinColumns = @JoinColumn(name = "txn_id")
    )
    private List<TransactionEntity> suspiciousTransactions;

    // Amendment Tracking
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "amends_sar_id")
    private SuspiciousActivityReport amendsSar; // If this SAR corrects a previous one
    
    private String amendmentReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = SarStatus.DRAFT;
        if (sarType == null) sarType = SarType.INITIAL;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
