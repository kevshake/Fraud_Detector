package com.posgateway.aml.dto.psp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PspResponse {
    private Long id;
    private String pspCode;
    private String legalName;
    private String status;
    private String billingPlan;
}
