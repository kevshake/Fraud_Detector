package com.posgateway.aml.entity.psp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * PSP (Payment Service Provider) Entity
 * Represents a client organization using the AML screening service
 */
@Entity
@Table(name = "psps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Psp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "psp_id")
    private Long pspId;

    @Column(name = "psp_code", unique = true, nullable = false, length = 50)
    private String pspCode; // e.g., "MPESA_KE", "PAYPAL_US"

    // Company Details
    @Column(name = "legal_name", nullable = false, length = 500)
    private String legalName;

    @Column(name = "trading_name", length = 500)
    private String tradingName;

    @Column(name = "country", nullable = false, length = 3)
    private String country;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    // Contact Information
    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_address", columnDefinition = "text")
    private String contactAddress;

    // Billing Configuration
    @Column(name = "billing_plan", length = 50)
    @Builder.Default
    private String billingPlan = "PAY_AS_YOU_GO"; // PAY_AS_YOU_GO, SUBSCRIPTION

    @Column(name = "billing_cycle", length = 20)
    @Builder.Default
    private String billingCycle = "MONTHLY"; // MONTHLY, QUARTERLY, YEARLY

    @Column(name = "payment_terms")
    @Builder.Default
    private Integer paymentTerms = 30; // Days

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    // Status
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, ACTIVE, SUSPENDED, TERMINATED

    @Column(name = "is_test_mode")
    @Builder.Default
    private Boolean isTestMode = false;

    // Theming
    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "primary_color", length = 20)
    private String primaryColor;

    @Column(name = "secondary_color", length = 20)
    private String secondaryColor;

    @Column(name = "accent_color", length = 20)
    private String accentColor;

    @Column(name = "font_family", length = 100)
    private String fontFamily;

    @Column(name = "font_size", length = 20)
    private String fontSize; // e.g., "14px", "1rem"

    @Column(name = "button_radius", length = 20)
    private String buttonRadius; // e.g., "4px", "0.5rem"

    @Column(name = "button_style", length = 50)
    private String buttonStyle; // e.g., "filled", "outline", "ghost"

    @Column(name = "nav_style", length = 50)
    private String navStyle; // e.g., "drawer", "topbar"

    // Timestamps
    @Column(name = "onboarded_at")
    @Builder.Default
    private LocalDateTime onboardedAt = LocalDateTime.now();

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "suspended_at")
    private LocalDateTime suspendedAt;

    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relationships
    @OneToMany(mappedBy = "psp", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PspUser> users = new ArrayList<>();

    // Helper Methods
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void activate() {
        this.status = "ACTIVE";
        this.activatedAt = LocalDateTime.now();
    }

    public void suspend(String reason) {
        this.status = "SUSPENDED";
        this.suspendedAt = LocalDateTime.now();
    }

    public void terminate() {
        this.status = "TERMINATED";
        this.terminatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
