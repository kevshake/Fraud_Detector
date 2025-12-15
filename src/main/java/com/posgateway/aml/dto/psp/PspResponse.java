package com.posgateway.aml.dto.psp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PspResponse {
    private Long id;
    private String pspCode;
    private String legalName;
    private String status;
    private String billingPlan;
}
