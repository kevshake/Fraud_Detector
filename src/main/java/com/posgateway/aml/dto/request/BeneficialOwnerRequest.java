package com.posgateway.aml.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for beneficial owner
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficialOwnerRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 500)
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Nationality is required")
    @Size(min = 3, max = 3, message = "Nationality must be 3-letter ISO code")
    private String nationality;

    @Size(min = 3, max = 3)
    private String countryOfResidence;

    @Size(max = 100)
    private String passportNumber;

    @Size(max = 100)
    private String nationalId;

    @NotNull(message = "Ownership percentage is required")
    @Min(value = 0, message = "Ownership percentage must be between 0 and 100")
    @Max(value = 100, message = "Ownership percentage must be between 0 and 100")
    private Integer ownershipPercentage;
}
