package com.posgateway.aml.service;

import com.posgateway.aml.entity.RolePermissionMapping;
import com.posgateway.aml.entity.User;
import com.posgateway.aml.model.Permission;
import com.posgateway.aml.model.UserRole;
import com.posgateway.aml.repository.RolePermissionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Permission Service
 * Handles granular permission checking and role-permission management
 */
@Service
public class PermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public PermissionService(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    /**
     * Check if current user has a specific permission
     */
    public boolean hasPermission(Permission permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        User user = (User) auth.getPrincipal();
        return hasPermission(user.getRole(), permission);
    }

    /**
     * Check if a role has a specific permission
     * Cached for performance
     */
    @Cacheable(value = "rolePermissions", key = "#role.name() + '_' + #permission.name()")
    public boolean hasPermission(UserRole role, Permission permission) {
        // ADMIN always has all permissions
        if (role == UserRole.ADMIN) {
            return true;
        }

        return rolePermissionRepository.existsByUserRoleAndPermission(role, permission);
    }

    /**
     * Get all permissions for a role
     */
    @Cacheable(value = "rolePermissions", key = "#role.name()")
    public Set<Permission> getPermissionsForRole(UserRole role) {
        if (role == UserRole.ADMIN) {
            return EnumSet.allOf(Permission.class);
        }

        return rolePermissionRepository.findPermissionsByUserRole(role);
    }

    /**
     * Grant a permission to a role
     */
    @Transactional
    public void grantPermission(UserRole role, Permission permission, String grantedBy, String notes) {
        if (!rolePermissionRepository.existsByUserRoleAndPermission(role, permission)) {
            RolePermissionMapping mapping = RolePermissionMapping.builder()
                    .userRole(role)
                    .permission(permission)
                    .grantedBy(grantedBy)
                    .grantedAt(LocalDateTime.now())
                    .notes(notes)
                    .build();

            rolePermissionRepository.save(mapping);
        }
    }

    /**
     * Revoke a permission from a role
     */
    @Transactional
    public void revokePermission(UserRole role, Permission permission) {
        rolePermissionRepository.deleteByUserRoleAndPermission(role, permission);
    }

    /**
     * Initialize default permissions for all roles
     */
    @Transactional
    public void initializeDefaultPermissions() {
        // MLRO permissions
        grantDefaultPermission(UserRole.MLRO, Permission.APPROVE_SAR);
        grantDefaultPermission(UserRole.MLRO, Permission.FILE_SAR);
        grantDefaultPermission(UserRole.MLRO, Permission.VIEW_CASES);
        grantDefaultPermission(UserRole.MLRO, Permission.VIEW_SAR);
        grantDefaultPermission(UserRole.MLRO, Permission.VIEW_PII);

        // COMPLIANCE_OFFICER permissions
        grantDefaultPermission(UserRole.COMPLIANCE_OFFICER, Permission.CREATE_CASES);
        grantDefaultPermission(UserRole.COMPLIANCE_OFFICER, Permission.ASSIGN_CASES);
        grantDefaultPermission(UserRole.COMPLIANCE_OFFICER, Permission.VIEW_SAR);
        grantDefaultPermission(UserRole.COMPLIANCE_OFFICER, Permission.CREATE_SAR);
        grantDefaultPermission(UserRole.COMPLIANCE_OFFICER, Permission.VIEW_PII);

        // INVESTIGATOR permissions
        grantDefaultPermission(UserRole.INVESTIGATOR, Permission.VIEW_CASES);
        grantDefaultPermission(UserRole.INVESTIGATOR, Permission.ADD_CASE_NOTES);
        grantDefaultPermission(UserRole.INVESTIGATOR, Permission.ADD_CASE_EVIDENCE);
        grantDefaultPermission(UserRole.INVESTIGATOR, Permission.CREATE_SAR);
        grantDefaultPermission(UserRole.INVESTIGATOR, Permission.VIEW_TRANSACTION_DETAILS);

        // ANALYST permissions
        grantDefaultPermission(UserRole.ANALYST, Permission.VIEW_CASES);
        grantDefaultPermission(UserRole.ANALYST, Permission.VIEW_TRANSACTION_DETAILS);
        grantDefaultPermission(UserRole.ANALYST, Permission.EXPORT_DATA);

        // SCREENING_ANALYST permissions
        grantDefaultPermission(UserRole.SCREENING_ANALYST, Permission.VIEW_SCREENING_RESULTS);
        grantDefaultPermission(UserRole.SCREENING_ANALYST, Permission.OVERRIDE_SCREENING_MATCH);
        grantDefaultPermission(UserRole.SCREENING_ANALYST, Permission.WHITELIST_ENTITY);

        // CASE_MANAGER permissions
        grantDefaultPermission(UserRole.CASE_MANAGER, Permission.VIEW_CASES);
        grantDefaultPermission(UserRole.CASE_MANAGER, Permission.ASSIGN_CASES);
        grantDefaultPermission(UserRole.CASE_MANAGER, Permission.ESCALATE_CASES);

        // AUDITOR permissions (read-only)
        grantDefaultPermission(UserRole.AUDITOR, Permission.VIEW_CASES);
        grantDefaultPermission(UserRole.AUDITOR, Permission.VIEW_SAR);
        grantDefaultPermission(UserRole.AUDITOR, Permission.VIEW_AUDIT_LOGS);

        // VIEWER permissions (minimal read-only)
        grantDefaultPermission(UserRole.VIEWER, Permission.VIEW_CASES);
    }

    private void grantDefaultPermission(UserRole role, Permission permission) {
        if (!rolePermissionRepository.existsByUserRoleAndPermission(role, permission)) {
            grantPermission(role, permission, "SYSTEM", "Default permission");
        }
    }
}
