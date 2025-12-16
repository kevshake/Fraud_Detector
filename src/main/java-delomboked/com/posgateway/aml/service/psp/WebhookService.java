package com.posgateway.aml.service.psp;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.posgateway.aml.entity.psp.WebhookSubscription;
import com.posgateway.aml.repository.WebhookSubscriptionRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

// @RequiredArgsConstructor removed
@Service

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(@Service.class);
public class WebhookService {

    private final WebhookSubscriptionRepository subscriptionRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public WebhookService(WebhookSubscriptionRepository subscriptionRepository, ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.objectMapper = objectMapper;
    }


    @Async("amlTaskExecutor") // Use our high-throughput pool
    public void sendWebhook(String eventType, Map<String, Object> payload) {
        log.debug("Processing webhooks for event: {}", eventType);

        List<WebhookSubscription> subscriptions = subscriptionRepository.findByEventTypeAndIsActiveTrue(eventType);

        for (WebhookSubscription sub : subscriptions) {
            try {
                String jsonPayload = objectMapper.writeValueAsString(payload);
                // In real app: Add HMAC signature header using sub.getSecretKey()

                log.info("Sending webhook to {} for event {}", sub.getCallbackUrl(), eventType);
                restTemplate.postForObject(sub.getCallbackUrl(), payload, String.class);

                // Reset failure count on success
                if (sub.getFailureCount() > 0) {
                    sub.setFailureCount(0);
                    subscriptionRepository.save(sub);
                }

            } catch (Exception e) {
                log.error("Failed to send webhook to {}: {}", sub.getCallbackUrl(), e.getMessage());
                sub.setFailureCount(sub.getFailureCount() + 1);
                if (sub.getFailureCount() > 5) {
                    sub.setActive(false);
                    log.warn("Disabling webhook subscription {} due to excessive failures", sub.getId());
                }
                subscriptionRepository.save(sub);
            }
        }
    }
}
