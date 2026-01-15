package com.posgateway.aml.model;

/**
 * User Roles for AML System
 * Defines the roles available in the system
 */
public enum UserRole {
    ADMIN,
    MLRO,                    // Money Laundering Reporting Officer
    COMPLIANCE_OFFICER,
    INVESTIGATOR,            // Case investigator
    ANALYST,
    SCREENING_ANALYST,       // Sanctions screening specialist
    CASE_MANAGER,            // Case workflow manager
    AUDITOR,
    VIEWER,                  // Read-only access
    PSP_ADMIN                // Manages PSP onboarding/configuration
}
