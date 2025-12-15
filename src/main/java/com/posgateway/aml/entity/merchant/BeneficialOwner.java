package com.posgateway.aml.entity.merchant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Beneficial Owner (UBO) entity
 * PII fields will be encrypted at application layer
 */
@Entity
@Table(name = "beneficial_owners")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficialOwner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_id")
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    // Identity (PII - should be encrypted)
    @Column(name = "full_name", nullable = false, length = 500)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "nationality", nullable = false, length = 3)
    private String nationality;

    @Column(name = "country_of_residence", length = 3)
    private String countryOfResidence;

    // Identification (encrypted)
    @Column(name = "passport_number", length = 100)
    private String passportNumber;

    @Column(name = "national_id", length = 100)
    private String nationalId;

    // Ownership
    @Column(name = "ownership_percentage", nullable = false)
    private Integer ownershipPercentage;

    // Screening Flags
    @Column(name = "is_pep")
    @Builder.Default
    private Boolean isPep = false;

    @Column(name = "is_sanctioned")
    @Builder.Default
    private Boolean isSanctioned = false;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "last_screened_at")
    private LocalDateTime lastScreenedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
