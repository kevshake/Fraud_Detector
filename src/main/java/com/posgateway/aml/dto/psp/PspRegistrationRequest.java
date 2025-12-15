package com.posgateway.aml.dto.psp;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PspRegistrationRequest {
    private String pspCode;
    private String legalName;
    private String tradingName;
    private String country;
    private String registrationNumber;
    private String taxId;
    private String contactEmail;
    private String contactPhone;
    private String contactAddress;
    private String billingPlan;
    private String currency;
    private Integer paymentTerms;
}
