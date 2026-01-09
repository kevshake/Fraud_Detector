package com.posgateway.aml.service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posgateway.aml.config.KafkaConfig;
import com.posgateway.aml.entity.compliance.ComplianceCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Producer service for broadcasting Case Management events.
 */
@Service
public class CaseEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(CaseEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public CaseEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish CASE_CREATED or CASE_UPDATED event.
     */
    public void publishCaseLifecycleEvent(ComplianceCase cCase, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("caseId", cCase.getId());
            event.put("caseReference", cCase.getCaseReference());
            event.put("status", cCase.getStatus());
            event.put("priority", cCase.getPriority());
            event.put("merchantId", cCase.getMerchantId());
            event.put("pspId", cCase.getPspId());
            event.put("timestamp", java.time.LocalDateTime.now().toString());

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaConfig.TOPIC_CASE_LIFECYCLE, String.valueOf(cCase.getId()), payload);
            logger.info("Published {} event for case {}", eventType, cCase.getCaseReference());

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize case event for {}", cCase.getCaseReference(), e);
        }
    }

    /**
     * Publish CASE_DECIDED event.
     */
    public void publishDecisionEvent(Long caseId, String decisionType, String justification, String username) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "CASE_DECIDED");
            event.put("caseId", caseId);
            event.put("decision", decisionType);
            event.put("justification", justification); // Brief or hashed if PII sensitive
            event.put("decidedBy", username);
            event.put("timestamp", java.time.LocalDateTime.now().toString());

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(KafkaConfig.TOPIC_CASE_DECISION, String.valueOf(caseId), payload);
            logger.info("Published CASE_DECISION event for case {}", caseId);

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize decision event for case {}", caseId, e);
        }
    }
}
