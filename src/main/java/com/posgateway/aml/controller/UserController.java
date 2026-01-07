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
@RequestMapping("/users")
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
        if (currentUser == null) {
            // Fallback for security disabled
            currentUser = userService.getSuperAdmin().orElse(null);
        }

        if (currentUser != null && !permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_USERS)) {
            throw new SecurityException("Not authorized");
        }

        Psp targetPsp = null;

        // If current user is Global Admin (PSP is null)
        if (currentUser == null || currentUser.getPsp() == null) {
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

    /**
     * Get user by ID
     * GET /api/v1/users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            currentUser = userService.getSuperAdmin().orElse(null);
        }

        if (currentUser != null && !permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_USERS)) {
            throw new SecurityException("Not authorized");
        }

        User user = userService.getUserById(id);
        
        // PSP scoping: PSP users can only see users from their PSP
        if (currentUser != null && currentUser.getPsp() != null) {
            if (user.getPsp() == null || !user.getPsp().getPspId().equals(currentUser.getPsp().getPspId())) {
                throw new SecurityException("Cannot access user from another PSP");
            }
        }
        
        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<User> createUser(@AuthenticationPrincipal User currentUser,
            @RequestBody CreateUserRequest req) {
        if (currentUser == null) {
            currentUser = userService.getSuperAdmin().orElse(null);
        }

        if (currentUser != null && !permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_USERS)) {
            throw new SecurityException("Not authorized");
        }

        Psp targetPsp = null;

        if (currentUser == null || currentUser.getPsp() == null) {
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

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest req,
            @AuthenticationPrincipal User currentUser) {
        // Authorization checks...
        if (currentUser != null && !permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_USERS)) {
            throw new SecurityException("Not authorized");
        }

        User updates = new User();
        updates.setFirstName(req.getFirstName());
        updates.setLastName(req.getLastName());
        updates.setEmail(req.getEmail());

        return ResponseEntity.ok(userService.updateUser(id, updates, req.getRoleId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @AuthenticationPrincipal User currentUser) {
        if (currentUser != null && !permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_USERS)) {
            throw new SecurityException("Not authorized");
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/{action}")
    public ResponseEntity<Void> toggleUserStatus(@PathVariable Long id, @PathVariable String action,
            @AuthenticationPrincipal User currentUser) {
        if (currentUser != null && !permissionService.hasPermission(currentUser.getRole(), Permission.MANAGE_USERS)) {
            throw new SecurityException("Not authorized");
        }

        boolean enable = "enable".equalsIgnoreCase(action);
        userService.toggleUserStatus(id, enable);
        return ResponseEntity.ok().build();
    }

    /**
     * Get current user profile
     * GET /api/v1/users/me
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            // Fallback for security disabled
            currentUser = userService.getSuperAdmin().orElse(null);
        }
        if (currentUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(currentUser);
    }

    public static class UpdateUserRequest {
        private String firstName;
        private String lastName;
        private String email;
        private Long roleId;

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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }
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
