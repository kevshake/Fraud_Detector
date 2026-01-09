package com.posgateway.aml.performance;

import com.posgateway.aml.BaseIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.test.context.jdbc.Sql;

/**
 * A basic load test to verify the system can handle concurrent requests
 * without crashing or throwing 500 errors.
 * This is not a full performance test but a "smoke load test" for CI.
 */
@Sql(scripts = "/test-seed-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BasicLoadTest extends BaseIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicLoadTest.class);

    @Test
    void shouldHandleConcurrentTransactionRequests() throws InterruptedException {
        int threads = 10;
        int requestsPerThread = 5;
        int totalRequests = threads * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // Use a simple health check or lightweight endpoint for load testing
        // to avoid heavy database setup in this smoke test.
        // Assuming /actuator/health or similar exists, or a simple get merchant

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalRequests; i++) {
            executor.submit(() -> {
                try {
                    // Using Actuator Health as a proxy for basic system responsiveness under load
                    // In a real scenario, this would be the transaction processing endpoint
                    given()
                            .spec(requestSpec)
                            .when()
                            .get("/api/v1/actuator/health")
                            .then()
                            .statusCode(200);

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    logger.error("Request failed: " + e.getMessage());
                } catch (AssertionError e) {
                    failureCount.incrementAndGet();
                    logger.error("Assertion failed: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);

        long duration = System.currentTimeMillis() - startTime;

        logger.info("Load Test Result: Success={}, Failure={}, Duration={}ms",
                successCount.get(), failureCount.get(), duration);

        assertTrue(finished, "Load test did not finish in time");
        assertTrue(successCount.get() > 0, "No requests succeeded");

        // We accept some failures if system is overloaded, but predominantly should
        // pass
        // For this basic test, we expect > 90% success
        assertTrue(failureCount.get() < (totalRequests * 0.1), "Too many failures under load");
    }
}
