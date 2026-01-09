package com.posgateway.aml.controller;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Grafana User Context Controller
 * Provides user context information for Grafana dashboards
 * Used for role-based access control and PSP filtering
 */
@RestController
@RequestMapping("/api/v1/grafana")
public class GrafanaUserContextController {

    private final UserRepository userRepository;

    @Autowired
    public GrafanaUserContextController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get user context for Grafana dashboards
     * Returns PSP code and user role for dashboard filtering
     * 
     * @return User context with psp_code and user_role
     */
    @GetMapping("/user-context")
    public ResponseEntity<Map<String, String>> getUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.ok(createAnonymousContext());
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        
        if (user == null) {
            return ResponseEntity.ok(createAnonymousContext());
        }

        Map<String, String> context = new HashMap<>();
        
        // Check if user is Platform Administrator
        boolean isPlatformAdmin = user.getRole() != null && 
                ("ADMIN".equals(user.getRole().getName()) || 
                 "APP_CONTROLLER".equals(user.getRole().getName()));
        
        if (isPlatformAdmin) {
            // Platform Administrator - can see all PSPs
            context.put("user_role", "PLATFORM_ADMIN");
            context.put("psp_code", "ALL");
            context.put("psp_id", "ALL");
            context.put("can_view_all_psps", "true");
        } else if (user.getPsp() != null) {
            // PSP User - can only see their PSP
            context.put("user_role", "PSP_USER");
            context.put("psp_code", user.getPsp().getPspCode() != null ? user.getPsp().getPspCode() : "UNKNOWN");
            context.put("psp_id", user.getPsp().getPspId() != null ? user.getPsp().getPspId().toString() : "0");
            context.put("can_view_all_psps", "false");
        } else {
            // User without PSP assignment
            context.put("user_role", "UNKNOWN");
            context.put("psp_code", "NONE");
            context.put("psp_id", "0");
            context.put("can_view_all_psps", "false");
        }
        
        return ResponseEntity.ok(context);
    }

    /**
     * Create anonymous context for unauthenticated users
     */
    private Map<String, String> createAnonymousContext() {
        Map<String, String> context = new HashMap<>();
        context.put("user_role", "ANONYMOUS");
        context.put("psp_code", "NONE");
        context.put("psp_id", "0");
        context.put("can_view_all_psps", "false");
        return context;
    }
}