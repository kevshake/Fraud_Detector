package com.posgateway.aml.service.psp;

import com.posgateway.aml.entity.psp.BillingRate;
import com.posgateway.aml.entity.psp.Invoice;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.repository.ApiUsageLogRepository;
import com.posgateway.aml.repository.BillingRateRepository;
import com.posgateway.aml.repository.InvoiceRepository;
import com.posgateway.aml.repository.PspRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private BillingRateRepository billingRateRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PspRepository pspRepository;

    @Mock
    private ApiUsageLogRepository apiUsageLogRepository;

    @InjectMocks
    private BillingService billingService;

    private Psp testPsp;

    @BeforeEach
    void setUp() {
        testPsp = Psp.builder()
                .pspId(1L)
                .pspCode("TEST_PSP")
                .paymentTerms(30)
                .currency("USD")
                .build();
    }

    @Test
    void testGetEffectiveRate_PspSpecific() {
        BillingRate rate = BillingRate.builder().baseRate(BigDecimal.ONE).build();
        when(billingRateRepository.findActiveRateForPsp(eq(1L), eq("TEST_SERVICE"), any(LocalDate.class)))
                .thenReturn(Optional.of(rate));

        Optional<BillingRate> result = billingService.getEffectiveRate(1L, "TEST_SERVICE");

        assertTrue(result.isPresent());
        assertEquals(BigDecimal.ONE, result.get().getBaseRate());
    }

    @Test
    void testCalculateUsageCost() {
        BillingRate rate = BillingRate.builder()
                .pricingModel("PER_REQUEST")
                .baseRate(BigDecimal.valueOf(0.50))
                .build();

        when(billingRateRepository.findActiveRateForPsp(eq(1L), eq("TEST_SERVICE"), any(LocalDate.class)))
                .thenReturn(Optional.of(rate));

        BigDecimal cost = billingService.calculateUsageCost(1L, "TEST_SERVICE", 10);

        assertEquals(BigDecimal.valueOf(5.00), cost);
    }

    @Test
    void testGenerateMonthlyInvoice() {
        LocalDate periodStart = LocalDate.of(2023, 10, 1);

        when(pspRepository.findById(1L)).thenReturn(Optional.of(testPsp));

        Object[] usageRow = new Object[] { "TEST_SERVICE", 100L, BigDecimal.valueOf(50.00) };
        when(apiUsageLogRepository.getUsageSummaryByService(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(usageRow));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> {
            Invoice inv = i.getArgument(0);
            return inv;
        });

        Invoice invoice = billingService.generateMonthlyInvoice(1L, periodStart);

        assertNotNull(invoice);
        assertEquals(BigDecimal.valueOf(50.00), invoice.getTotalAmount());
        assertEquals(1, invoice.getLineItems().size());
        assertEquals("TEST_SERVICE", invoice.getLineItems().get(0).getServiceType());
    }
}
