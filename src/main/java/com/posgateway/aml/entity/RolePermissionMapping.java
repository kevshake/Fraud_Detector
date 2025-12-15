package com.posgateway.aml.entity;

import com.posgateway.aml.model.Permission;
import com.posgateway.aml.model.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Role-Permission Mapping Entity
 * Maps UserRoles to specific Permissions for granular access control
 */
@Entity
@Table(name = "role_permissions", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_role", "permission" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRole userRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Permission permission;

    @Column(name = "granted_by")
    private String grantedBy; // Username who granted this permission

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @Column(columnDefinition = "TEXT")
    private String notes; // Reason for granting

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) {
            grantedAt = LocalDateTime.now();
        }
    }
}
