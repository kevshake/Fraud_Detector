package com.posgateway.aml.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Transaction Statistics Service
 * Maintains transaction amounts and counts in Redis/Aerospike for fast AML velocity checks
 * Automatically tracks: merchant counts, PAN counts, amount sums, velocity metrics
 * 
 * This service keeps running totals updated in real-time for instant AML risk assessment
 */
@Service
@SuppressWarnings("null") // String.format never returns null, Redis operations are safe
public class TransactionStatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionStatisticsService.class);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Value("${aerospike.enabled:false}")
    private boolean aerospikeEnabled;

    @Value("${transaction.stats.key.prefix:aml:stats}")
    private String keyPrefix;

    @Value("${transaction.stats.ttl.hours:168}") // 7 days default TTL
    private int ttlHours;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    /**
     * Record a transaction for statistics tracking
     * Updates counts and amounts in Redis/Aerospike automatically
     * 
     * @param merchantId Merchant ID
     * @param panHash PAN hash (SHA-256)
     * @param amountCents Transaction amount in cents
     * @param terminalId Terminal ID (optional)
     */
    public void recordTransaction(String merchantId, String panHash, Long amountCents, String terminalId) {
        if (amountCents == null || amountCents <= 0) {
            return; // Skip invalid amounts
        }

        LocalDateTime now = LocalDateTime.now();
        String dateKey = now.format(DATE_FORMATTER);
        String hourKey = now.format(HOUR_FORMATTER);

        try {
            // Update merchant statistics
            if (merchantId != null) {
                updateMerchantStats(merchantId, amountCents, dateKey, hourKey);
            }

            // Update PAN statistics
            if (panHash != null) {
                updatePanStats(panHash, amountCents, dateKey, hourKey, terminalId);
            }

        } catch (Exception e) {
            logger.warn("Error recording transaction statistics: {}", e.getMessage());
            // Don't fail transaction processing if stats fail
        }
    }

    /**
     * Get merchant transaction count for time window
     * 
     * @param merchantId Merchant ID
     * @param hours Number of hours to look back
     * @return Transaction count
     */
    public long getMerchantTransactionCount(String merchantId, int hours) {
        if (merchantId == null) {
            return 0;
        }

        try {
            if (redisEnabled && redisTemplate != null) {
                return getMerchantCountFromRedis(merchantId, hours);
            } else if (aerospikeEnabled) {
                return getMerchantCountFromAerospike(merchantId, hours);
            }
        } catch (Exception e) {
            logger.debug("Error getting merchant count: {}", e.getMessage());
        }

        return 0;
    }

    /**
     * Get merchant transaction amount sum for time window
     * 
     * @param merchantId Merchant ID
     * @param hours Number of hours to look back
     * @return Amount sum in cents
     */
    public long getMerchantAmountSum(String merchantId, int hours) {
        if (merchantId == null) {
            return 0;
        }

        try {
            if (redisEnabled && redisTemplate != null) {
                return getMerchantAmountFromRedis(merchantId, hours);
            } else if (aerospikeEnabled) {
                return getMerchantAmountFromAerospike(merchantId, hours);
            }
        } catch (Exception e) {
            logger.debug("Error getting merchant amount: {}", e.getMessage());
        }

        return 0;
    }

    /**
     * Get PAN transaction count for time window
     * 
     * @param panHash PAN hash
     * @param hours Number of hours to look back
     * @return Transaction count
     */
    public long getPanTransactionCount(String panHash, int hours) {
        if (panHash == null) {
            return 0;
        }

        try {
            if (redisEnabled && redisTemplate != null) {
                return getPanCountFromRedis(panHash, hours);
            } else if (aerospikeEnabled) {
                return getPanCountFromAerospike(panHash, hours);
            }
        } catch (Exception e) {
            logger.debug("Error getting PAN count: {}", e.getMessage());
        }

        return 0;
    }

    /**
     * Get PAN transaction amount sum for time window
     * 
     * @param panHash PAN hash
     * @param hours Number of hours to look back
     * @return Amount sum in cents
     */
    public long getPanAmountSum(String panHash, int hours) {
        if (panHash == null) {
            return 0;
        }

        try {
            if (redisEnabled && redisTemplate != null) {
                return getPanAmountFromRedis(panHash, hours);
            } else if (aerospikeEnabled) {
                return getPanAmountFromAerospike(panHash, hours);
            }
        } catch (Exception e) {
            logger.debug("Error getting PAN amount: {}", e.getMessage());
        }

        return 0;
    }

    /**
     * Get cumulative amount for PAN over days
     * 
     * @param panHash PAN hash
     * @param days Number of days to look back
     * @return Cumulative amount in cents
     */
    public long getPanCumulativeAmount(String panHash, int days) {
        if (panHash == null) {
            return 0;
        }

        try {
            if (redisEnabled && redisTemplate != null) {
                return getPanCumulativeFromRedis(panHash, days);
            } else if (aerospikeEnabled) {
                return getPanCumulativeFromAerospike(panHash, days);
            }
        } catch (Exception e) {
            logger.debug("Error getting PAN cumulative: {}", e.getMessage());
        }

        return 0;
    }

    // ==================== Redis Implementation ====================

    private void updateMerchantStats(String merchantId, Long amountCents, String dateKey, String hourKey) {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        // Hourly counters
        String hourCountKey = String.format("%s:merchant:%s:count:%s", keyPrefix, merchantId, hourKey);
        String hourAmountKey = String.format("%s:merchant:%s:amount:%s", keyPrefix, merchantId, hourKey);

        redisTemplate.opsForValue().increment(hourCountKey);
        redisTemplate.opsForValue().increment(hourAmountKey, amountCents);
        redisTemplate.expire(hourCountKey, ttlHours, TimeUnit.HOURS);
        redisTemplate.expire(hourAmountKey, ttlHours, TimeUnit.HOURS);

        // Daily counters
        String dayCountKey = String.format("%s:merchant:%s:count:%s", keyPrefix, merchantId, dateKey);
        String dayAmountKey = String.format("%s:merchant:%s:amount:%s", keyPrefix, merchantId, dateKey);

        redisTemplate.opsForValue().increment(dayCountKey);
        redisTemplate.opsForValue().increment(dayAmountKey, amountCents);
        redisTemplate.expire(dayCountKey, ttlHours, TimeUnit.HOURS);
        redisTemplate.expire(dayAmountKey, ttlHours, TimeUnit.HOURS);

        // Rolling 24h window (for fast lookup)
        String rolling24hCountKey = String.format("%s:merchant:%s:count:24h", keyPrefix, merchantId);
        String rolling24hAmountKey = String.format("%s:merchant:%s:amount:24h", keyPrefix, merchantId);

        redisTemplate.opsForValue().increment(rolling24hCountKey);
        redisTemplate.opsForValue().increment(rolling24hAmountKey, amountCents);
        redisTemplate.expire(rolling24hCountKey, 25, TimeUnit.HOURS); // Slightly longer than 24h
        redisTemplate.expire(rolling24hAmountKey, 25, TimeUnit.HOURS);
    }

    private void updatePanStats(String panHash, Long amountCents, String dateKey, String hourKey, String terminalId) {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }

        // Hourly counters
        String hourCountKey = String.format("%s:pan:%s:count:%s", keyPrefix, panHash, hourKey);
        String hourAmountKey = String.format("%s:pan:%s:amount:%s", keyPrefix, panHash, hourKey);

        redisTemplate.opsForValue().increment(hourCountKey);
        redisTemplate.opsForValue().increment(hourAmountKey, amountCents);
        redisTemplate.expire(hourCountKey, ttlHours, TimeUnit.HOURS);
        redisTemplate.expire(hourAmountKey, ttlHours, TimeUnit.HOURS);

        // Daily counters
        String dayCountKey = String.format("%s:pan:%s:count:%s", keyPrefix, panHash, dateKey);
        String dayAmountKey = String.format("%s:pan:%s:amount:%s", keyPrefix, panHash, dateKey);

        redisTemplate.opsForValue().increment(dayCountKey);
        redisTemplate.opsForValue().increment(dayAmountKey, amountCents);
        redisTemplate.expire(dayCountKey, ttlHours, TimeUnit.HOURS);
        redisTemplate.expire(dayAmountKey, ttlHours, TimeUnit.HOURS);

        // Rolling windows
        String rolling1hCountKey = String.format("%s:pan:%s:count:1h", keyPrefix, panHash);
        String rolling1hAmountKey = String.format("%s:pan:%s:amount:1h", keyPrefix, panHash);
        String rolling24hCountKey = String.format("%s:pan:%s:count:24h", keyPrefix, panHash);
        String rolling24hAmountKey = String.format("%s:pan:%s:amount:24h", keyPrefix, panHash);
        String rolling7dCountKey = String.format("%s:pan:%s:count:7d", keyPrefix, panHash);
        String rolling7dAmountKey = String.format("%s:pan:%s:amount:7d", keyPrefix, panHash);
        String rolling30dCountKey = String.format("%s:pan:%s:count:30d", keyPrefix, panHash);
        String rolling30dAmountKey = String.format("%s:pan:%s:amount:30d", keyPrefix, panHash);

        redisTemplate.opsForValue().increment(rolling1hCountKey);
        redisTemplate.opsForValue().increment(rolling1hAmountKey, amountCents);
        redisTemplate.expire(rolling1hCountKey, 2, TimeUnit.HOURS);
        redisTemplate.expire(rolling1hAmountKey, 2, TimeUnit.HOURS);

        redisTemplate.opsForValue().increment(rolling24hCountKey);
        redisTemplate.opsForValue().increment(rolling24hAmountKey, amountCents);
        redisTemplate.expire(rolling24hCountKey, 25, TimeUnit.HOURS);
        redisTemplate.expire(rolling24hAmountKey, 25, TimeUnit.HOURS);

        redisTemplate.opsForValue().increment(rolling7dCountKey);
        redisTemplate.opsForValue().increment(rolling7dAmountKey, amountCents);
        redisTemplate.expire(rolling7dCountKey, 8, TimeUnit.DAYS);
        redisTemplate.expire(rolling7dAmountKey, 8, TimeUnit.DAYS);

        redisTemplate.opsForValue().increment(rolling30dCountKey);
        redisTemplate.opsForValue().increment(rolling30dAmountKey, amountCents);
        redisTemplate.expire(rolling30dCountKey, 31, TimeUnit.DAYS);
        redisTemplate.expire(rolling30dAmountKey, 31, TimeUnit.DAYS);

        // Track distinct terminals
        if (terminalId != null) {
            String terminalSetKey = String.format("%s:pan:%s:terminals:30d", keyPrefix, panHash);
            redisTemplate.opsForSet().add(terminalSetKey, terminalId);
            redisTemplate.expire(terminalSetKey, 31, TimeUnit.DAYS);
        }
    }

    private long getMerchantCountFromRedis(String merchantId, int hours) {
        if (!redisEnabled || redisTemplate == null) {
            return 0;
        }
        
        if (hours <= 24) {
            String key = String.format("%s:merchant:%s:count:24h", keyPrefix, merchantId);
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0;
        }

        // Sum hourly keys for custom windows
        long total = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < hours; i++) {
            String hourKey = now.minusHours(i).format(HOUR_FORMATTER);
            String key = String.format("%s:merchant:%s:count:%s", keyPrefix, merchantId, hourKey);
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                total += Long.parseLong(value);
            }
        }
        return total;
    }

    private long getMerchantAmountFromRedis(String merchantId, int hours) {
        if (!redisEnabled || redisTemplate == null) {
            return 0;
        }
        
        if (hours <= 24) {
            String key = String.format("%s:merchant:%s:amount:24h", keyPrefix, merchantId);
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0;
        }

        // Sum hourly keys
        long total = 0;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < hours; i++) {
            String hourKey = now.minusHours(i).format(HOUR_FORMATTER);
            String key = String.format("%s:merchant:%s:amount:%s", keyPrefix, merchantId, hourKey);
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                total += Long.parseLong(value);
            }
        }
        return total;
    }

    private long getPanCountFromRedis(String panHash, int hours) {
        if (!redisEnabled || redisTemplate == null) {
            return 0;
        }
        
        switch (hours) {
            case 1:
                String key1h = String.format("%s:pan:%s:count:1h", keyPrefix, panHash);
                String value1h = redisTemplate.opsForValue().get(key1h);
                return value1h != null ? Long.parseLong(value1h) : 0;
            case 24:
                String key24h = String.format("%s:pan:%s:count:24h", keyPrefix, panHash);
                String value24h = redisTemplate.opsForValue().get(key24h);
                return value24h != null ? Long.parseLong(value24h) : 0;
            default:
                // Sum hourly keys
                long total = 0;
                LocalDateTime now = LocalDateTime.now();
                for (int i = 0; i < hours && i < 168; i++) { // Max 7 days
                    String hourKey = now.minusHours(i).format(HOUR_FORMATTER);
                    String key = String.format("%s:pan:%s:count:%s", keyPrefix, panHash, hourKey);
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        total += Long.parseLong(value);
                    }
                }
                return total;
        }
    }

    private long getPanAmountFromRedis(String panHash, int hours) {
        if (!redisEnabled || redisTemplate == null) {
            return 0;
        }
        
        switch (hours) {
            case 1:
                String key1h = String.format("%s:pan:%s:amount:1h", keyPrefix, panHash);
                String value1h = redisTemplate.opsForValue().get(key1h);
                return value1h != null ? Long.parseLong(value1h) : 0;
            case 24:
                String key24h = String.format("%s:pan:%s:amount:24h", keyPrefix, panHash);
                String value24h = redisTemplate.opsForValue().get(key24h);
                return value24h != null ? Long.parseLong(value24h) : 0;
            default:
                // Sum hourly keys
                long total = 0;
                LocalDateTime now = LocalDateTime.now();
                for (int i = 0; i < hours && i < 168; i++) {
                    String hourKey = now.minusHours(i).format(HOUR_FORMATTER);
                    String key = String.format("%s:pan:%s:amount:%s", keyPrefix, panHash, hourKey);
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        total += Long.parseLong(value);
                    }
                }
                return total;
        }
    }

    private long getPanCumulativeFromRedis(String panHash, int days) {
        if (!redisEnabled || redisTemplate == null) {
            return 0;
        }
        
        switch (days) {
            case 7:
                String key7d = String.format("%s:pan:%s:amount:7d", keyPrefix, panHash);
                String value7d = redisTemplate.opsForValue().get(key7d);
                return value7d != null ? Long.parseLong(value7d) : 0;
            case 30:
                String key30d = String.format("%s:pan:%s:amount:30d", keyPrefix, panHash);
                String value30d = redisTemplate.opsForValue().get(key30d);
                return value30d != null ? Long.parseLong(value30d) : 0;
            default:
                // Sum daily keys
                long total = 0;
                LocalDateTime now = LocalDateTime.now();
                for (int i = 0; i < days && i < 90; i++) {
                    String dateKey = now.minusDays(i).format(DATE_FORMATTER);
                    String key = String.format("%s:pan:%s:amount:%s", keyPrefix, panHash, dateKey);
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        total += Long.parseLong(value);
                    }
                }
                return total;
        }
    }

    // ==================== Aerospike Implementation ====================

    private long getMerchantCountFromAerospike(String merchantId, int hours) {
        // Aerospike implementation would go here
        // For now, return 0 as placeholder
        return 0;
    }

    private long getMerchantAmountFromAerospike(String merchantId, int hours) {
        // Aerospike implementation
        return 0;
    }

    private long getPanCountFromAerospike(String panHash, int hours) {
        // Aerospike implementation
        return 0;
    }

    private long getPanAmountFromAerospike(String panHash, int hours) {
        // Aerospike implementation
        return 0;
    }

    private long getPanCumulativeFromAerospike(String panHash, int days) {
        // Aerospike implementation
        return 0;
    }

    /**
     * Get distinct terminal count for PAN
     */
    public long getDistinctTerminalCount(String panHash, int days) {
        if (panHash == null || !redisEnabled || redisTemplate == null) {
            return 0;
        }

        try {
            String key = String.format("%s:pan:%s:terminals:%dd", keyPrefix, panHash, days);
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            logger.debug("Error getting distinct terminal count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Clear statistics (for testing or reset)
     */
    public void clearStatistics(String merchantId, String panHash) {
        if (!redisEnabled || redisTemplate == null) {
            return;
        }
        
        try {
            if (merchantId != null) {
                redisTemplate.delete(String.format("%s:merchant:%s:*", keyPrefix, merchantId));
            }
            if (panHash != null) {
                redisTemplate.delete(String.format("%s:pan:%s:*", keyPrefix, panHash));
            }
        } catch (Exception e) {
            logger.warn("Error clearing statistics: {}", e.getMessage());
        }
    }
}

