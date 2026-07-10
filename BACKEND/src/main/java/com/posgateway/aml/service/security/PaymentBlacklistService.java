package com.posgateway.aml.service.security;

import com.posgateway.aml.repository.security.PaymentBlacklistRepository;
import com.posgateway.aml.service.cache.FeatureCacheService;
import org.springframework.stereotype.Service;

/**
 * Checks PAN/terminal/IP blacklist from Redis cache with Postgres fallback.
 */
@Service
public class PaymentBlacklistService {

    private final FeatureCacheService featureCacheService;
    private final PaymentBlacklistRepository blacklistRepository;

    public PaymentBlacklistService(FeatureCacheService featureCacheService,
                                   PaymentBlacklistRepository blacklistRepository) {
        this.featureCacheService = featureCacheService;
        this.blacklistRepository = blacklistRepository;
    }

    public boolean isBlacklisted(String type, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (featureCacheService.isBlacklisted(type, value)) {
            return true;
        }
        return blacklistRepository.existsByEntryTypeAndEntryValueAndActiveTrue(type, value);
    }
}
