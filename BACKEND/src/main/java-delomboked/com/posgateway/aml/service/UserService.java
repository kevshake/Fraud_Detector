package com.posgateway.aml.service;



import com.posgateway.aml.entity.Role;
import com.posgateway.aml.entity.User;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.repository.RoleRepository;
import com.posgateway.aml.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// @RequiredArgsConstructor removed
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    public List<User> getUsersByPsp(Psp psp) {
        if (psp == null) {
            // For System Admins, maybe show all? Or just system users?
            // For now, let's assume NULL psp means getting System Users.
            // If we want ALL users across all PSPs, we'd need a different method or flag.
            return userRepository.findByPspIsNull();
        }
        return userRepository.findByPsp(psp);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public User createUser(User user, Long roleId, Psp psp) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        // Validate Role belongs to PSP
        if (role.getPsp() != null && !role.getPsp().equals(psp)) {
            // Check if it's not a system role (system roles have null psp and can be
            // assigned to anyone?)
            // Usually System Roles are for System Users, but maybe we allow "View Only"
            // system role to be assigned to PSP users?
            // For strict isolation: PSP Users must have PSP Roles OR specific Global Roles
            // if allowed.
            // Simplified: PSP Users must have Role.psp == user.psp OR Role.psp == null
            // (Global)
            if (psp != null && !role.isSystemRole()) { // If user is PSP user, but role is another PSP's role
                throw new IllegalArgumentException("Cannot assign a role from a different PSP");
            }
        }

        // If user is System (psp=null), Role must be System (psp=null)
        if (psp == null && !role.isSystemRole()) {
            throw new IllegalArgumentException("System users cannot have PSP-specific roles");
        }

        user.setRole(role);
        user.setPsp(psp);
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long userId, User updates, Long roleId) {
        User user = getUserById(userId);

        if (updates.getFirstName() != null)
            user.setFirstName(updates.getFirstName());
        if (updates.getLastName() != null)
            user.setLastName(updates.getLastName());
        if (updates.getEmail() != null)
            user.setEmail(updates.getEmail()); // Should check uniqueness if changed

        if (roleId != null) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
            // Validate role scoping matches user's PSP
            if (user.getPsp() != null && role.getPsp() != null && !role.getPsp().equals(user.getPsp())) {
                throw new IllegalArgumentException("Role does not belong to user's PSP");
            }
            user.setRole(role);
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
