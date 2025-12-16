package com.posgateway.aml.controller.psp;

import com.posgateway.aml.dto.psp.InvoiceGenerationRequest;
import com.posgateway.aml.dto.psp.InvoiceResponse;
import com.posgateway.aml.entity.psp.BillingRate;
import com.posgateway.aml.entity.psp.Invoice;
import com.posgateway.aml.service.psp.BillingService;
import com.posgateway.aml.mapper.InvoiceMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {

    private final BillingService billingService;
    private final InvoiceMapper invoiceMapper;

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
        return ResponseEntity.ok(invoiceMapper.toResponse(invoice));
    }

    // TODO: Implement list invoices endpoint in BillingService or Repository
    // wrapper if needed
    // For now we only implemented generation directly.
    // We will skip "list invoices" for this iteration as it requires service
    // expansion not in "Existing" blocks
}
