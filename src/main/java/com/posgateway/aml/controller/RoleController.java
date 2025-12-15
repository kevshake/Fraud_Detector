package com.posgateway.aml.controller;

import com.posgateway.aml.entity.Role;
import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.model.Permission;
import com.posgateway.aml.repository.PspRepository;
import com.posgateway.aml.service.PermissionService;
import com.posgateway.aml.service.RoleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final PspRepository pspRepository;

    @GetMapping
    public ResponseEntity<List<Role>> listRoles(@AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long pspId) {
        // Anyone can list roles available to them (for dropdowns etc)
        // But let's restrict full management listing if needed.

        Psp targetPsp = null;

        if (currentUser.getPsp() == null) {
            if (pspId != null) {
                targetPsp = pspRepository.findById(pspId)
                        .orElseThrow(() -> new IllegalArgumentException("PSP not found"));
            }
            // If pspId is null, list System Roles? Or maybe all System Roles are available
            // to everyone?
            // Let's assume listRoles matches the context of the user.
        } else {
            targetPsp = currentUser.getPsp();
        }

        return ResponseEntity.ok(roleService.getRolesForPsp(targetPsp));
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@AuthenticationPrincipal User currentUser,
            @RequestBody CreateRoleRequest req) {
        if (!permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_ROLES)) {
            throw new SecurityException("Not authorized");
        }

        Psp targetPsp = null;

        if (currentUser.getPsp() == null) {
            if (req.getPspId() != null) {
                targetPsp = pspRepository.findById(req.getPspId())
                        .orElseThrow(() -> new IllegalArgumentException("PSP not found"));
            }
        } else {
            targetPsp = currentUser.getPsp();
            if (req.getPspId() != null && !java.util.Objects.equals(req.getPspId(), targetPsp.getPspId())) {
                throw new SecurityException("Cannot create role for another PSP");
            }
        }

        return ResponseEntity
                .ok(roleService.createRole(req.getName(), req.getDescription(), targetPsp, req.getPermissions()));
    }

    @Data
    public static class CreateRoleRequest {
        private String name;
        private String description;
        private Long pspId;
        private Set<Permission> permissions;
    }
}
