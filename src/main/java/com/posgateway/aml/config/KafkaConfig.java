package com.posgateway.aml.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Configuration
 * Defines topics and producer/consumer settings for AML events.
 */
@Configuration
public class KafkaConfig {

    public static final String TOPIC_CASE_LIFECYCLE = "aml.case.lifecycle";
    public static final String TOPIC_CASE_DECISION = "aml.case.decision";
    public static final String TOPIC_COMPLIANCE_ALERT = "aml.compliance.alert";

    /**
     * Topic for Case creation, status changes, and assignment events.
     */
    @Bean
    public NewTopic caseLifecycleTopic() {
        return TopicBuilder.name(TOPIC_CASE_LIFECYCLE)
                .partitions(3)
                .replicas(1) // Adjust based on broker setup (1 for localhost dev)
                .build();
    }

    /**
     * Topic for final regulatory decisions (SAR filed, Case Closed).
     */
    @Bean
    public NewTopic caseDecisionTopic() {
        return TopicBuilder.name(TOPIC_CASE_DECISION)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Topic for raw alerts that trigger risk scores or cases.
     */
    @Bean
    public NewTopic complianceAlertTopic() {
        return TopicBuilder.name(TOPIC_COMPLIANCE_ALERT)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
