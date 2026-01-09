package com.posgateway.aml.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis Automatic Initialization Service
 * Automatically initializes Redis keys, structures, and ensures connection is ready
 * Runs on application startup
 */
@Service
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = false)
@SuppressWarnings("null") // String concatenations with keyPrefix are safe, Redis operations accept non-null strings
public class RedisInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(RedisInitializationService.class);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    @Value("${redis.auto.init.enabled:true}")
    private boolean autoInitEnabled;

    @Value("${redis.keys.prefix:aml}")
    private String keyPrefix;

    /**
     * Automatically initialize Redis structures on startup
     */
    @PostConstruct
    public void initialize() {
        if (!redisEnabled || !autoInitEnabled) {
            logger.info("Redis auto-initialization is disabled");
            return;
        }

        if (redisTemplate == null) {
            logger.warn("RedisTemplate is not available - Redis may not be configured");
            return;
        }

        logger.info("🚀 Starting automatic Redis initialization...");

        try {
            // Test connection
            testConnection();

            // Initialize cache keys
            initializeCacheKeys();

            // Initialize counters
            initializeCounters();

            // Initialize sets/lists if needed
            initializeDataStructures();

            logger.info("✅ Redis automatic initialization completed successfully");

        } catch (Exception e) {
            logger.error("❌ Error during Redis auto-initialization: {}", e.getMessage(), e);
            // Don't fail startup - application can work without Redis
        }
    }

    /**
     * Test Redis connection
     */
    private void testConnection() {
        try {
            redisTemplate.opsForValue().set(keyPrefix + ":init:test", "ok", 10, TimeUnit.SECONDS);
            String value = redisTemplate.opsForValue().get(keyPrefix + ":init:test");
            if ("ok".equals(value)) {
                logger.info("✅ Redis connection verified");
            } else {
                logger.warn("⚠️ Redis connection test failed - unexpected value");
            }
        } catch (Exception e) {
            logger.error("❌ Redis connection test failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Initialize cache keys with default TTL
     */
    private void initializeCacheKeys() {
        logger.info("Initializing Redis cache keys...");

        try {
            // Create cache key patterns (keys will be created on first use)
            // Set default TTL for cache keys (24 hours)
            long defaultTtl = TimeUnit.HOURS.toSeconds(24);

            // Initialize cache key patterns
            String[] cacheKeys = {
                    keyPrefix + ":cache:modelConfig",
                    keyPrefix + ":cache:aggregateFeatures",
                    keyPrefix + ":cache:sanctions",
                    keyPrefix + ":cache:merchant",
                    keyPrefix + ":cache:transaction"
            };

            for (String key : cacheKeys) {
                // Set empty value with TTL to ensure key structure exists
                redisTemplate.opsForValue().set(key, "", defaultTtl, TimeUnit.SECONDS);
                // Delete immediately - we just wanted to verify structure
                redisTemplate.delete(key);
            }

            logger.info("✅ Cache key patterns initialized");

        } catch (Exception e) {
            logger.warn("Error initializing cache keys: {}", e.getMessage());
        }
    }

    /**
     * Initialize counters (for metrics/statistics)
     */
    private void initializeCounters() {
        logger.info("Initializing Redis counters...");

        try {
            // Initialize counters with 0 if they don't exist
            String[] counters = {
                    keyPrefix + ":counter:transactions:total",
                    keyPrefix + ":counter:transactions:fraud",
                    keyPrefix + ":counter:transactions:aml",
                    keyPrefix + ":counter:screening:total",
                    keyPrefix + ":counter:screening:matches"
            };

            for (String counter : counters) {
                if (Boolean.FALSE.equals(redisTemplate.hasKey(counter))) {
                    redisTemplate.opsForValue().set(counter, "0");
                }
            }

            logger.info("✅ Counters initialized");

        } catch (Exception e) {
            logger.warn("Error initializing counters: {}", e.getMessage());
        }
    }

    /**
     * Initialize data structures (sets, lists, etc.)
     */
    private void initializeDataStructures() {
        logger.info("Initializing Redis data structures...");

        try {
            // Initialize sets for tracking
            String[] sets = {
                    keyPrefix + ":set:blocked:pans",
                    keyPrefix + ":set:blocked:merchants",
                    keyPrefix + ":set:high:risk:countries"
            };

            for (String setKey : sets) {
                // Verify set exists (will be created on first add)
                Long size = redisTemplate.opsForSet().size(setKey);
                if (size == null) {
                    // Set doesn't exist, create empty set
                    redisTemplate.opsForSet().add(setKey, "init");
                    redisTemplate.opsForSet().remove(setKey, "init");
                }
            }

            // Initialize lists for queues
            String[] lists = {
                    keyPrefix + ":list:alerts:pending",
                    keyPrefix + ":list:cases:queue"
            };

            for (String listKey : lists) {
                // Verify list exists (will be created on first push)
                Long size = redisTemplate.opsForList().size(listKey);
                if (size == null) {
                    // List doesn't exist, create empty list
                    redisTemplate.opsForList().rightPush(listKey, "init");
                    redisTemplate.opsForList().remove(listKey, 1, "init");
                }
            }

            logger.info("✅ Data structures initialized");

        } catch (Exception e) {
            logger.warn("Error initializing data structures: {}", e.getMessage());
        }
    }

    /**
     * Get Redis connection info
     */
    public String getConnectionInfo() {
        try {
            if (redisTemplate == null) {
                return "Redis not configured";
            }

            Set<String> keys = redisTemplate.keys(keyPrefix + ":*");
            int keyCount = keys != null ? keys.size() : 0;

            return String.format("Redis connected - Keys with prefix '%s': %d", keyPrefix, keyCount);
        } catch (Exception e) {
            return "Redis error: " + e.getMessage();
        }
    }
}

