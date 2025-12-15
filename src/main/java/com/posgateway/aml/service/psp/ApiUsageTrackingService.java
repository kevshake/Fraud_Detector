package com.posgateway.aml.service.psp;

import com.posgateway.aml.dto.psp.ApiUsageEvent;
import com.posgateway.aml.entity.psp.ApiUsageLog;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.entity.psp.PspUser;
import com.posgateway.aml.repository.ApiUsageLogRepository;
import com.posgateway.aml.repository.PspRepository;
import com.posgateway.aml.repository.PspUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiUsageTrackingService {

    private final ApiUsageLogRepository apiUsageLogRepository;
    private final PspRepository pspRepository;
    private final PspUserRepository pspUserRepository;
    private final BillingService billingService;

    @Async
    @Transactional
    public void logRequest(ApiUsageEvent event) {
        log.debug("Logging API usage for PSP: {}", event.getPspId());

        try {
            Psp psp = pspRepository.findById(event.getPspId())
                    .orElseThrow(() -> new IllegalArgumentException("PSP not found: " + event.getPspId()));

            PspUser user = null;
            if (event.getUserId() != null) {
                user = pspUserRepository.findById(event.getUserId()).orElse(null);
            }

            // Calculate cost
            BigDecimal cost = billingService.calculateUsageCost(event.getPspId(), event.getServiceType(), 1);

            ApiUsageLog usageLog = ApiUsageLog.builder()
                    .psp(psp)
                    .user(user)
                    .endpoint(event.getEndpoint())
                    .httpMethod(event.getHttpMethod())
                    .requestTimestamp(event.getTimestamp())
                    .responseStatus(event.getResponseStatus())
                    .responseTimeMs(event.getResponseTimeMs())
                    .serviceType(event.getServiceType())
                    .billable(cost.compareTo(BigDecimal.ZERO) > 0)
                    .costAmount(cost)
                    .costCurrency("USD") // Should ideally come from billing rate
                    .requestId(event.getRequestId())
                    .merchantId(event.getMerchantId())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .externalProvider(event.getExternalProvider())
                    .externalCost(event.getExternalCost())
                    .requestSizeBytes(event.getRequestSizeBytes())
                    .responseSizeBytes(event.getResponseSizeBytes())
                    .errorMessage(event.getErrorMessage())
                    .build();

            apiUsageLogRepository.save(usageLog);

        } catch (Exception e) {
            log.error("Failed to log API usage event", e);
            // Don't rethrow, strictly logging shouldn't fail the main flow if it's async
        }
    }
}
