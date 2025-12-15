package com.posgateway.aml.entity;

import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.model.Permission;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

/**
 * Dynamic Role Entity
 * Supports custom roles for both System (null PSP) and specific PSPs.
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psp_id")
    private Psp psp; // NULL = System global role, NOT NULL = PSP specific role

    @ElementCollection(targetClass = Permission.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission")
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    // Helper to check for system role
    public boolean isSystemRole() {
        return psp == null;
    }
}
