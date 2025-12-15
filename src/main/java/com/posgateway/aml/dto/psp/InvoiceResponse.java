package com.posgateway.aml.dto.psp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private String invoiceNumber;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal amount;
    private String status;
    private List<LineItem> lineItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItem {
        private String serviceType;
        private String description;
        private int quantity;
        private BigDecimal total;
    }
}
