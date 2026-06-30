package com.posgateway.aml.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "amlTaskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Configuration for High Throughput (target 30K req/sec support)
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(50000); // 50K buffer
        executor.setThreadNamePrefix("AML-Executor-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "backgroundTaskExecutor")
    public Executor backgroundTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("BG-Executor-");
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated pool for asynchronous transaction fraud processing.
     * AsyncFraudDetectionOrchestrator#processTransactionAsync is annotated
     * {@code @Async("transactionExecutor")}; without this bean every ingest
     * request that fell through to the async (non-ultra) path failed with
     * NoSuchBeanDefinitionException AFTER the transaction had already been
     * persisted, surfacing as a 500 to the caller.
     */
    @Bean(name = "transactionExecutor")
    public Executor transactionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(25);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(20000);
        executor.setThreadNamePrefix("Txn-Executor-");
        executor.initialize();
        return executor;
    }

    /**
     * Dedicated pool for billing/metering event publishing.
     * MeteringEventPublisher methods are annotated {@code @Async("meteringExecutor")};
     * without this bean each fire-and-forget metering event raised an
     * NoSuchBeanDefinitionException on the publishing thread.
     */
    @Bean(name = "meteringExecutor")
    public Executor meteringExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("Metering-Executor-");
        executor.initialize();
        return executor;
    }
}
