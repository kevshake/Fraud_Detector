package com.posgateway.aml.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for merchant onboarding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantOnboardingRequest {

    // Basic Information
    @NotBlank(message = "Legal name is required")
    @Size(max = 500, message = "Legal name must not exceed 500 characters")
    private String legalName;

    @Size(max = 500, message = "Trading name must not exceed 500 characters")
    private String tradingName;

    @NotBlank(message = "Country is required")
    @Size(min = 3, max = 3, message = "Country must be 3-letter ISO code")
    private String country;

    @NotBlank(message = "Registration number is required")
    @Size(max = 100, message = "Registration number must not exceed 100 characters")
    private String registrationNumber;

    @Size(max = 100)
    private String taxId;

    // Business Details
    @NotBlank(message = "MCC is required")
    @Size(max = 10)
    private String mcc;

    @Size(max = 50)
    private String businessType; // CORPORATION, LLC, PARTNERSHIP, SOLE_PROPRIETOR

    @Min(value = 0, message = "Expected monthly volume must be positive")
    private Long expectedMonthlyVolume; // in cents

    @Size(max = 50)
    private String transactionChannel; // ONLINE, IN_STORE, MOBILE

    @Size(max = 500)
    private String website;

    // Address
    private String addressStreet;
    private String addressCity;
    private String addressState;
    private String addressPostalCode;
    private String addressCountry;

    // Operational Data
    private List<String> operatingCountries;

    @Past(message = "Registration date must be in the past")
    private LocalDate registrationDate;

    // Beneficial Owners
    @NotEmpty(message = "At least one beneficial owner is required")
    @Valid
    private List<BeneficialOwnerRequest> beneficialOwners;
}
