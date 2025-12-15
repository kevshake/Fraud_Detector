package com.posgateway.aml.dto.psp;

import lombok.Data;

@Data
public class InvoiceGenerationRequest {
    private Long pspId;
    private int month;
    private int year;
}
