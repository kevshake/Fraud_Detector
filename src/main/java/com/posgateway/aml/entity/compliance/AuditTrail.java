package com.posgateway.aml.entity.compliance;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Immutable audit trail entity
 * No updates or deletes allowed
 */
@Entity
@Table(name = "audit_trail")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    // Entity
    @Column(name = "merchant_id")
    private Long merchantId;

    // Action
    @Column(name = "action", nullable = false, length = 100)
    private String action; // ONBOARDED, SCREENED, APPROVED, REJECTED, UPDATED

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy; // User ID or SYSTEM

    @Column(name = "performed_at", nullable = false)
    @Builder.Default
    private LocalDateTime performedAt = LocalDateTime.now();

    // Evidence (immutable JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence", columnDefinition = "jsonb")
    private Map<String, Object> evidence;

    // Rules Version
    @Column(name = "rules_version", length = 50)
    private String rulesVersion;

    // Decision
    @Column(name = "decision", length = 50)
    private String decision;

    @Column(name = "decision_reason", columnDefinition = "text")
    private String decisionReason;

    // Metadata
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;
}
