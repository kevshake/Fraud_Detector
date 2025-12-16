package com.posgateway.aml.controller;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.model.Permission;
import com.posgateway.aml.repository.PspRepository;
import com.posgateway.aml.service.PermissionService;
import com.posgateway.aml.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RequiredArgsConstructor removed
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final PermissionService permissionService;
    private final PspRepository pspRepository;

    public UserController(UserService userService, PermissionService permissionService, PspRepository pspRepository) {
        this.userService = userService;
        this.permissionService = permissionService;
        this.pspRepository = pspRepository;
    }

    @GetMapping
    public ResponseEntity<List<User>> listUsers(@AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long pspId) {
        if (!permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_USERS)) {
            throw new SecurityException("Not authorized");
        }

        Psp targetPsp = null;

        // If current user is Global Admin (PSP is null)
        if (currentUser.getPsp() == null) {
            if (pspId != null) {
                targetPsp = pspRepository.findById(pspId)
                        .orElseThrow(() -> new IllegalArgumentException("PSP not found"));
            }
        } else {
            // PSP Admin/User: Can only see own PSP
            targetPsp = currentUser.getPsp();
            if (pspId != null && !pspId.equals(targetPsp.getPspId())) {
                throw new SecurityException("Cannot access other PSP's users");
            }
        }

        return ResponseEntity.ok(userService.getUsersByPsp(targetPsp));
    }

    @PostMapping
    public ResponseEntity<User> createUser(@AuthenticationPrincipal User currentUser,
            @RequestBody CreateUserRequest req) {
        if (!permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_USERS)) {
            throw new SecurityException("Not authorized");
        }

        Psp targetPsp = null;

        if (currentUser.getPsp() == null) {
            if (req.getPspId() != null) {
                targetPsp = pspRepository.findById(req.getPspId())
                        .orElseThrow(() -> new IllegalArgumentException("PSP not found"));
            }
            // If req.getPspId is null, creating a System Admin (allowed for Global Admin)
        } else {
            targetPsp = currentUser.getPsp();
            if (req.getPspId() != null && !java.util.Objects.equals(req.getPspId(), targetPsp.getPspId())) {
                throw new SecurityException("Cannot create user for another PSP");
            }
        }

        User newUser = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .passwordHash(req.getPassword()) // Service will encode
                .build();

        return ResponseEntity.ok(userService.createUser(newUser, req.getRoleId(), targetPsp));
    }

    public static class CreateUserRequest {
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private String password;
        private Long roleId;
        private Long pspId;

        public CreateUserRequest() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public Long getPspId() {
            return pspId;
        }

        public void setPspId(Long pspId) {
            this.pspId = pspId;
        }
    }
}
