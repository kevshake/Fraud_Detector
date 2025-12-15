package com.posgateway.aml.service.aml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.model.ScreeningResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class SumsubAmlServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    @Spy
    private SumsubAmlService sumsubAmlService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(sumsubAmlService, "enabled", true);
        ReflectionTestUtils.setField(sumsubAmlService, "apiUrl", "http://sumsub-api");
        ReflectionTestUtils.setField(sumsubAmlService, "apiKey", "token");
        ReflectionTestUtils.setField(sumsubAmlService, "apiSecret", "secret");
    }

    @Test
    void testScreenBatch() throws ExecutionException, InterruptedException {
        Merchant m1 = new Merchant();
        m1.setMerchantId(1L);
        m1.setLegalName("Merchant One");

        Merchant m2 = new Merchant();
        m2.setMerchantId(2L);
        m2.setLegalName("Merchant Two");

        ScreeningResult r1 = new ScreeningResult();
        r1.setStatus(ScreeningResult.ScreeningStatus.CLEAR);
        ScreeningResult r2 = new ScreeningResult();
        r2.setStatus(ScreeningResult.ScreeningStatus.MATCH); // REVIEW might not exist or be different

        // Mock internal call if possible, or mock external dependencies
        // Since screenMerchantWithSumsub is public, we can spy it
        doReturn(r1).when(sumsubAmlService).screenMerchantWithSumsub(m1);
        doReturn(r2).when(sumsubAmlService).screenMerchantWithSumsub(m2);

        List<Merchant> merchants = Arrays.asList(m1, m2);
        CompletableFuture<List<ScreeningResult>> future = sumsubAmlService.screenBatch(merchants);

        List<ScreeningResult> results = future.get();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals(ScreeningResult.ScreeningStatus.CLEAR, results.get(0).getStatus());
        assertEquals(ScreeningResult.ScreeningStatus.MATCH, results.get(1).getStatus());
    }
}
