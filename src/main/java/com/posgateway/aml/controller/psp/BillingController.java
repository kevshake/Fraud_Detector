package com.posgateway.aml.controller.psp;

import com.posgateway.aml.dto.psp.InvoiceGenerationRequest;
import com.posgateway.aml.dto.psp.InvoiceResponse;
import com.posgateway.aml.entity.psp.BillingRate;
import com.posgateway.aml.entity.psp.Invoice;
import com.posgateway.aml.service.psp.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Slf4j
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/rates")
    public ResponseEntity<BillingRate> getRate(@RequestParam Long pspId, @RequestParam String serviceType) {
        Optional<BillingRate> rate = billingService.getEffectiveRate(pspId, serviceType);
        return rate.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/invoices/generate")
    public ResponseEntity<InvoiceResponse> generateInvoice(@RequestBody InvoiceGenerationRequest request) {
        log.info("Manual invoice generation trigger for PSP {}", request.getPspId());
        LocalDate periodStart = LocalDate.of(request.getYear(), request.getMonth(), 1);
        Invoice invoice = billingService.generateMonthlyInvoice(request.getPspId(), periodStart);
        return ResponseEntity.ok(mapToInvoiceResponse(invoice));
    }

    // TODO: Implement list invoices endpoint in BillingService or Repository
    // wrapper if needed
    // For now we only implemented generation directly.
    // We will skip "list invoices" for this iteration as it requires service
    // expansion not in "Existing" blocks

    private InvoiceResponse mapToInvoiceResponse(Invoice invoice) {
        List<InvoiceResponse.LineItem> items = invoice.getLineItems().stream()
                .map(item -> InvoiceResponse.LineItem.builder()
                        .serviceType(item.getServiceType())
                        .description(item.getDescription())
                        .quantity(item.getQuantity())
                        .total(item.getLineTotal())
                        .build())
                .collect(Collectors.toList());

        return InvoiceResponse.builder()
                .invoiceNumber(invoice.getInvoiceNumber())
                .periodStart(invoice.getBillingPeriodStart())
                .periodEnd(invoice.getBillingPeriodEnd())
                .amount(invoice.getTotalAmount())
                .status(invoice.getStatus())
                .lineItems(items)
                .build();
    }
}
