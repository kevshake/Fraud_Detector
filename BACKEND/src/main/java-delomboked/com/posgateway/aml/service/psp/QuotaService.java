package com.posgateway.aml.service.psp;



import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.repository.PspRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service to handle PSP rate limiting/quotas.
 * Implements a simple fixed-window rate limiter (per minute).
 */
// @RequiredArgsConstructor removed
@Service

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(@Service.class);
public class QuotaService {

    private final PspRepository pspRepository;

    public QuotaService(PspRepository pspRepository) {
        this.pspRepository = pspRepository;
    }


    // key: pspCode, value: RequestCounter
    private final Map<String, RequestCounter> limits = new ConcurrentHashMap<>();

    // Defaults - normally configured in DB/Properties
    private static final int LIMIT_ENTERPRISE = 1000; // req/min
    private static final int LIMIT_BASIC = 100; // req/min
    private static final int LIMIT_DEFAULT = 50; // req/min

    public boolean isRequestAllowed(String pspCode) {
        // Compute limit based on cached usage or look up fresh
        // For performance, we'd cache the plan. Here we hit DB for correctness MVP.
        // In high-throughput, use a @Cacheable or local map for plan lookup.

        Psp psp = pspRepository.findByPspCode(pspCode).orElse(null);
        if (psp == null) {
            log.warn("Quota check failed: Unknown PSP code {}", pspCode);
            return false;
        }

        int limit = getLimitForPlan(psp.getBillingPlan());

        return limits.computeIfAbsent(pspCode, k -> new RequestCounter())
                .tryAcquire(limit);
    }

    private int getLimitForPlan(String plan) {
        if (plan == null)
            return LIMIT_DEFAULT;
        if ("ENTERPRISE".equalsIgnoreCase(plan) || "SUBSCRIPTION".equalsIgnoreCase(plan))
            return LIMIT_ENTERPRISE;
        if ("PAY_AS_YOU_GO".equalsIgnoreCase(plan))
            return LIMIT_BASIC;
        return LIMIT_DEFAULT;
    }

    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = Instant.now().getEpochSecond() / 60; // Minute window

        public synchronized boolean tryAcquire(int maxRequests) {
            long currentWindow = Instant.now().getEpochSecond() / 60;
            if (currentWindow > windowStart) {
                windowStart = currentWindow;
                count.set(0);
            }

            return count.incrementAndGet() <= maxRequests;
        }
    }
}
