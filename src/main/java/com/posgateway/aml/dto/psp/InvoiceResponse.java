package com.posgateway.aml.dto.psp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class InvoiceResponse {
    private String invoiceNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal amount;
    private String status;
    private List<LineItem> lineItems;

    @Data
    @Builder
    public static class LineItem {
        private String serviceType;
        private String description;
        private int quantity;
        private BigDecimal total;
    }
}
