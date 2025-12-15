package com.posgateway.aml.entity.psp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * PSP User Entity
 * Users belong to a specific PSP and authenticate via email
 */
@Entity
@Table(name = "psp_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Deprecated // Replaced by generic User entity with PSP association
public class PspUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psp_id", nullable = false)
    private Psp psp;

    // Identity
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false, length = 500)
    private String fullName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Role & Permissions
    @Column(name = "role", length = 50)
    @Builder.Default
    private String role = "OPERATOR"; // ADMIN, OPERATOR, VIEWER

    @Column(name = "permissions", columnDefinition = "text[]")
    private String[] permissions;

    // Status
    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, DELETED

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "verification_token")
    private String verificationToken;

    // Security
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip", length = 45)
    private String lastLoginIp;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_expires")
    private LocalDateTime passwordResetExpires;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Helper Methods
    public boolean isActive() {
        return "ACTIVE".equals(status) && !isLocked();
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void recordSuccessfulLogin(String ipAddress) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ipAddress;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusHours(1);
        }
    }

    public boolean hasPermission(String permission) {
        if (permissions == null)
            return false;
        for (String perm : permissions) {
            if (perm.equals(permission) || perm.equals("*")) {
                return true;
            }
        }
        return false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
