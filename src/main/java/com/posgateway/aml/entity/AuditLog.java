package com.posgateway.aml.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Enhanced Audit Log Entity
 * Captures comprehensive details about user actions for regulatory compliance
 */
@Entity
@Table(name = "audit_logs_enhanced")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who
    @Column(nullable = false)
    private String userId; // User ID or "SYSTEM"
    
    @Column(nullable = false)
    private String username;
    
    private String userRole;

    // What
    @Column(nullable = false)
    private String actionType; // VIEW, CREATE, UPDATE, DELETE, EXPORT, LOGIN, LOGOUT
    
    @Column(nullable = false)
    private String entityType; // CASE, SAR, TRANSACTION, USER, RULE
    
    @Column(nullable = false)
    private String entityId;

    // Change Tracking
    @Column(columnDefinition = "TEXT")
    private String beforeValue; // JSON of state before change
    
    @Column(columnDefinition = "TEXT")
    private String afterValue; // JSON of state after change

    // When
    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Where
    private String ipAddress;
    private String sessionId;
    private String userAgent;

    // Why
    private String reason; // Mandatory for sensitive actions like overrides

    // Result
    @Builder.Default
    private boolean success = true;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage; // If failed

    // Integrity
    @Column(updatable = false)
    private String checksum; // Hash of the record for tamper detection (HMAC)

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

