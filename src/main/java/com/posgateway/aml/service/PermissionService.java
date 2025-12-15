package com.posgateway.aml.service;

import com.posgateway.aml.model.Permission;
import com.posgateway.aml.model.UserRole;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Permission Service
 * Manages mapping between Roles and Permissions
 */
@Service
public class PermissionService {

    private static final Map<UserRole, Set<Permission>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        // ADMIN - All permissions
        ROLE_PERMISSIONS.put(UserRole.ADMIN, EnumSet.allOf(Permission.class));

        // MLRO - Money Laundering Reporting Officer
        Set<Permission> mlroPermissions = EnumSet.allOf(Permission.class);
        mlroPermissions.remove(Permission.MANAGE_USERS); // MLRO typically doesn't manage users
        mlroPermissions.remove(Permission.CONFIGURE_SYSTEM);
        ROLE_PERMISSIONS.put(UserRole.MLRO, mlroPermissions);

        // PSP_ADMIN - manage PSP onboarding and theming
        Set<Permission> pspAdminPerms = EnumSet.of(
                Permission.MANAGE_PSP,
                Permission.MANAGE_PSP_THEME,
                Permission.MANAGE_USERS,
                Permission.MANAGE_ROLES,
                Permission.VIEW_AUDIT_LOGS
        );
        ROLE_PERMISSIONS.put(UserRole.PSP_ADMIN, pspAdminPerms);

        // COMPLIANCE_OFFICER
        Set<Permission> complianceOfficerPermissions = EnumSet.of(
            Permission.VIEW_CASES, Permission.CREATE_CASES, Permission.ASSIGN_CASES, 
            Permission.CLOSE_CASES, Permission.ESCALATE_CASES, Permission.REOPEN_CASES,
            Permission.ADD_CASE_NOTES, Permission.ADD_CASE_EVIDENCE,
            Permission.VIEW_SAR, Permission.CREATE_SAR,
            Permission.VIEW_PII, Permission.VIEW_TRANSACTION_DETAILS,
            Permission.VIEW_SCREENING_RESULTS, Permission.MANAGE_WATCHLISTS,
            Permission.WHITELIST_ENTITY, Permission.OVERRIDE_SCREENING_MATCH,
            Permission.VIEW_AUDIT_LOGS
        );
        ROLE_PERMISSIONS.put(UserRole.COMPLIANCE_OFFICER, complianceOfficerPermissions);

        // INVESTIGATOR
        Set<Permission> investigatorPermissions = EnumSet.of(
            Permission.VIEW_CASES, Permission.ADD_CASE_NOTES, Permission.ADD_CASE_EVIDENCE,
            Permission.ESCALATE_CASES,
            Permission.VIEW_TRANSACTION_DETAILS, Permission.VIEW_PII,
            Permission.VIEW_SCREENING_RESULTS
        );
        ROLE_PERMISSIONS.put(UserRole.INVESTIGATOR, investigatorPermissions);

        // ANALYST
        Set<Permission> analystPermissions = EnumSet.of(
            Permission.VIEW_CASES, Permission.ADD_CASE_NOTES,
            Permission.VIEW_TRANSACTION_DETAILS,
            Permission.VIEW_SCREENING_RESULTS
        );
        ROLE_PERMISSIONS.put(UserRole.ANALYST, analystPermissions);
        
        // SCREENING_ANALYST
        Set<Permission> screeningAnalystPermissions = EnumSet.of(
            Permission.VIEW_SCREENING_RESULTS, Permission.WHITELIST_ENTITY,
            Permission.OVERRIDE_SCREENING_MATCH,
            Permission.VIEW_TRANSACTION_DETAILS
        );
        ROLE_PERMISSIONS.put(UserRole.SCREENING_ANALYST, screeningAnalystPermissions);

        // CASE_MANAGER
        Set<Permission> caseManagerPermissions = EnumSet.of(
            Permission.VIEW_CASES, Permission.ASSIGN_CASES, Permission.REOPEN_CASES,
            Permission.ESCALATE_CASES, Permission.CLOSE_CASES,
            Permission.VIEW_AUDIT_LOGS
        );
        ROLE_PERMISSIONS.put(UserRole.CASE_MANAGER, caseManagerPermissions);

        // AUDITOR
        Set<Permission> auditorPermissions = EnumSet.of(
            Permission.VIEW_CASES, Permission.VIEW_SAR,
            Permission.VIEW_TRANSACTION_DETAILS, Permission.VIEW_AUDIT_LOGS,
            Permission.VIEW_SCREENING_RESULTS
        );
        ROLE_PERMISSIONS.put(UserRole.AUDITOR, auditorPermissions);

        // VIEWER
        Set<Permission> viewerPermissions = EnumSet.of(
            Permission.VIEW_CASES,
            Permission.VIEW_TRANSACTION_DETAILS
        );
        ROLE_PERMISSIONS.put(UserRole.VIEWER, viewerPermissions);
    }

    /**
     * Get permissions for a specific role
     */
    public Set<Permission> getPermissionsForRole(UserRole role) {
        return ROLE_PERMISSIONS.getOrDefault(role, EnumSet.noneOf(Permission.class));
    }

    /**
     * Check if a role has a specific permission
     */
    public boolean hasPermission(UserRole role, Permission permission) {
        Set<Permission> permissions = getPermissionsForRole(role);
        return permissions.contains(permission);
    }
}

