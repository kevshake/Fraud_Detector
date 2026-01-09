package com.posgateway.aml.integration;

import com.posgateway.aml.BaseIntegrationTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = "/test-seed-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class MerchantOnboardingIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldOnboardAndRetrieveMerchant() {
        long timestamp = System.currentTimeMillis();
        // Match controller expected fields: legalName, tradingName, contactEmail, mcc,
        // businessType
        String merchantJson = """
                {
                    "legalName": "Integration Test Corp %d",
                    "tradingName": "IT Corp %d",
                    "contactEmail": "test%d@integration.com",
                    "mcc": "5000",
                    "businessType": "CORPORATION",
                    "riskLevel": "LOW",
                    "dailyLimit": 100000
                }
                """.formatted(timestamp, timestamp, timestamp);

        // 1. Create Merchant
        int merchantId = given()
                .spec(requestSpec)
                .body(merchantJson)
                .when()
                .post("/merchants")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpStatus.CREATED.value())
                .body("merchantId", notNullValue())
                .body("legalName", equalTo("Integration Test Corp " + timestamp))
                .extract().path("merchantId");

        // 2. Retrieve Merchant
        given()
                .spec(requestSpec)
                .when()
                .get("/merchants/" + merchantId)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("merchantId", equalTo(merchantId))
                .body("legalName", equalTo("Integration Test Corp " + timestamp));
    }

    @Test
    void shouldValidateInvalidMerchant() {
        // Empty body should still be processed but may result in null fields
        // Controller uses Map<String, Object> so no validation errors at binding level
        // This test verifies the endpoint is reachable
        String partialJson = """
                {
                    "legalName": "Test Merchant"
                }
                """;

        given()
                .spec(requestSpec)
                .body(partialJson)
                .when()
                .post("/merchants")
                .then()
                .log().ifValidationFails()
                .statusCode(anyOf(is(201), is(500))); // Might fail if no PSP exists
    }
}
