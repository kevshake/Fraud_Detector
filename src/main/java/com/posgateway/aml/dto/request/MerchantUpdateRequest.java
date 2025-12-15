package com.posgateway.aml.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantUpdateRequest {

    @Size(max = 100)
    private String legalName;

    @Size(max = 100)
    private String tradingName;

    private String businessType;
    private String mcc;
    private Long expectedMonthlyVolume;
    private String website;

    // Address fields
    private String addressStreet;
    private String addressCity;
    private String addressState;
    private String addressPostalCode;
    private String addressCountry;

    private String contactEmail;
    private String contactPhone;

    private List<String> operatingCountries;

    private List<BeneficialOwnerRequest> newBeneficialOwners;
}
