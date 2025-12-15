package com.posgateway.aml.service.risk;

import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.model.RiskLevel;
import com.posgateway.aml.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;

class TransactionLimitServiceTest {

    @Mock
    private NotificationService notificationService;

    private TransactionLimitService transactionLimitService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionLimitService = new TransactionLimitService(notificationService);
    }

    @Test
    void testCheckAndAlertOnBreach_Exceeded() {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(1L);
        RiskLevel riskLevel = RiskLevel.HIGH; // Limit is 10k
        BigDecimal currentVolume = new BigDecimal("15000.00");

        transactionLimitService.checkAndAlertOnBreach(merchant, riskLevel, currentVolume);

        verify(notificationService).sendAlert(contains("breached"), eq("HIGH"));
    }

    @Test
    void testCheckAndAlertOnBreach_NotExceeded() {
        Merchant merchant = new Merchant();
        merchant.setMerchantId(1L);
        RiskLevel riskLevel = RiskLevel.HIGH; // Limit is 10k
        BigDecimal currentVolume = new BigDecimal("5000.00");

        transactionLimitService.checkAndAlertOnBreach(merchant, riskLevel, currentVolume);

        verify(notificationService, never()).sendAlert(anyString(), anyString());
    }
}
