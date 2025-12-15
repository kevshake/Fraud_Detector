package com.posgateway.aml.controller.auth;

import com.posgateway.aml.model.Permission;
import com.posgateway.aml.model.UserRole;
import com.posgateway.aml.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RolePermissionController {

    private final PermissionService permissionService;

    @GetMapping("/roles")
    public ResponseEntity<List<UserRole>> getRoles() {
        return ResponseEntity.ok(Arrays.asList(UserRole.values()));
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getPermissions() {
        return ResponseEntity.ok(Arrays.asList(Permission.values()));
    }

    @GetMapping("/role-permissions")
    public ResponseEntity<Set<Permission>> getRolePermissions(@RequestParam("role") UserRole role) {
        return ResponseEntity.ok(permissionService.getPermissionsForRole(role));
    }
}

