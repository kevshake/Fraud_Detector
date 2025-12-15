package com.posgateway.aml.entity.merchant;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Merchant entity for AML screening and onboarding
 */
@Entity
@Table(name = "merchants", indexes = {
        @Index(name = "idx_merchant_status", columnList = "status"),
        @Index(name = "idx_merchant_country", columnList = "address_country"),
        @Index(name = "idx_merchant_mcc", columnList = "mcc")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id")
    private Long merchantId;

    // Basic Information
    @Column(name = "legal_name", nullable = false, length = 500)
    private String legalName;

    @Column(name = "trading_name", length = 500)
    private String tradingName;

    @Column(name = "country", nullable = false, length = 3)
    private String country;

    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.posgateway.aml.config.security.PiiMaskingSerializer.class)
    @Column(name = "registration_number", nullable = false, length = 100)
    private String registrationNumber;

    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.posgateway.aml.config.security.PiiMaskingSerializer.class)
    @Column(name = "tax_id", length = 100)
    private String taxId;

    // Kenyan Specific Fields (Phase 29)
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.posgateway.aml.config.security.PiiMaskingSerializer.class)
    @Column(name = "kra_pin", length = 50)
    private String kraPin;

    @Column(name = "cr12_number", length = 100)
    private String cr12Number;

    @Column(name = "is_pep", nullable = false)
    @Builder.Default
    private boolean isPep = false;

    // Business Details
    @Column(name = "mcc", nullable = false, length = 10)
    private String mcc;

    @Column(name = "business_type", length = 50)
    private String businessType;

    @Column(name = "expected_monthly_volume")
    private Long expectedMonthlyVolume;

    @Column(name = "transaction_channel", length = 50)
    private String transactionChannel;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    // Address
    @Column(name = "address_street", length = 500)
    private String addressStreet;

    @Column(name = "address_city", length = 200)
    private String addressCity;

    @Column(name = "address_state", length = 100)
    private String addressState;

    @Column(name = "address_postal_code", length = 20)
    private String addressPostalCode;

    @Column(name = "address_country", length = 3)
    private String addressCountry;

    // Operational Data
    @Column(name = "operating_countries", columnDefinition = "text[]")
    private String[] operatingCountries;

    @Column(name = "registration_date")
    private LocalDate registrationDate;

    // Status
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING_SCREENING";

    // Timestamps
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "last_screened_at")
    private LocalDateTime lastScreenedAt;

    @Column(name = "next_screening_due")
    private LocalDate nextScreeningDue;

    // Relationships
    @OneToMany(mappedBy = "merchant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BeneficialOwner> beneficialOwners = new ArrayList<>();

    /**
     * Check if merchant is new (registered within configured months)
     */
    public boolean isNew() {
        if (createdAt == null) {
            return true;
        }
        // Consider merchant new if created within last 30 days
        return createdAt.isAfter(LocalDateTime.now().minusDays(30));
    }

    /**
     * Update next screening due date (7 days from now - weekly)
     */
    public void updateNextScreeningDue() {
        this.lastScreenedAt = LocalDateTime.now();
        this.nextScreeningDue = LocalDate.now().plusDays(7);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
