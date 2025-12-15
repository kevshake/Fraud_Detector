package com.posgateway.aml.service.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posgateway.aml.entity.compliance.AuditTrail;
import com.posgateway.aml.repository.AuditTrailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AuditService {

    private final AuditTrailRepository auditTrailRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditTrailRepository auditTrailRepository, ObjectMapper objectMapper) {
        this.auditTrailRepository = auditTrailRepository;
        this.objectMapper = objectMapper;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String actionType, Long merchantId, String userId, String details, Object payload) {
        log.debug("Logging audit action: {}", actionType);

        java.util.Map<String, Object> evidenceMap;
        if (payload instanceof java.util.Map) {
            evidenceMap = (java.util.Map<String, Object>) payload;
        } else {
            // Wrap payload if not a map
            evidenceMap = java.util.Collections.singletonMap("data", payload);
        }

        AuditTrail audit = AuditTrail.builder()
                .merchantId(merchantId)
                .action(actionType)
                .performedBy(userId)
                .decisionReason(details) // Mapping details to decisionReason
                .evidence(evidenceMap)
                .build();

        auditTrailRepository.save(audit);
    }
}
