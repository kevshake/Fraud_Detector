package com.posgateway.aml.config;

import com.posgateway.aml.service.PermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Permission Initializer
 * Runs on application startup to initialize default role permissions
 * 
 * Order(10): Runs after database migrations but before main app logic
 */
@Component
@Order(10)
public class PermissionInitializer implements CommandLineRunner {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PermissionInitializer.class);

    private final com.posgateway.aml.service.RoleService roleService;

    public PermissionInitializer(com.posgateway.aml.service.RoleService roleService) {
        this.roleService = roleService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing default role permissions...");

        try {
            roleService.initDefaultRoles();
            log.info("Default role permissions initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize permissions: {}", e.getMessage());
            // Don't fail startup - permissions can be initialized later
        }
    }
}
