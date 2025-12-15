package com.posgateway.aml.service.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SlackServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SlackService slackService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        slackService = new SlackService(restTemplate);
        ReflectionTestUtils.setField(slackService, "enabled", true);
        ReflectionTestUtils.setField(slackService, "webhookUrl", "http://slack-webhook");
    }

    @Test
    void testSendAlert() {
        slackService.sendAlert("Test Alert", "HIGH");
        verify(restTemplate, times(1)).postForEntity(eq("http://slack-webhook"), any(), eq(String.class));
    }

    @Test
    void testSendAlertDisabled() {
        ReflectionTestUtils.setField(slackService, "enabled", false);
        slackService.sendAlert("Test Alert", "HIGH");
        verify(restTemplate, times(0)).postForEntity(anyString(), any(), any());
    }
}
