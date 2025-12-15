package com.posgateway.aml.service.compliance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.posgateway.aml.dto.compliance.CreateSarRequest;
import com.posgateway.aml.dto.compliance.SarResponse;
import com.posgateway.aml.dto.compliance.UpdateSarRequest;
import com.posgateway.aml.entity.compliance.SuspiciousActivityReport;
import com.posgateway.aml.model.SarStatus;
import com.posgateway.aml.mapper.SarMapper;
import com.posgateway.aml.repository.SuspiciousActivityReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ComplianceReportingService {

    private final SuspiciousActivityReportRepository sarRepository;
    private final ObjectMapper objectMapper;
    private final SarMapper sarMapper;

    public ComplianceReportingService(SuspiciousActivityReportRepository sarRepository,
            ObjectMapper objectMapper,
            SarMapper sarMapper) {
        this.sarRepository = sarRepository;
        this.objectMapper = objectMapper;
        this.sarMapper = sarMapper;
    }

    @Transactional
    public SarResponse createSar(CreateSarRequest request) {
        log.info("Creating new SAR (caseId={} merchantId={})", request.getCaseId(), request.getMerchantId());

        SuspiciousActivityReport sar = SuspiciousActivityReport.builder()
                .sarReference("SAR-" + UUID.randomUUID())
                .status(SarStatus.DRAFT)
                .narrative(request.getNarrative())
                .suspiciousActivityType("GENERAL")
                .jurisdiction("UNKNOWN")
                .build();

        sar = sarRepository.save(sar);
        return sarMapper.toResponse(sar);
    }

    @Transactional
    public SarResponse updateSar(Long id, UpdateSarRequest request) {
        SuspiciousActivityReport sar = sarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SAR not found: " + id));

        if (request.getStatus() != null) {
            sar.setStatus(SarStatus.valueOf(request.getStatus()));
        }
        if (request.getNarrative() != null) {
            sar.setNarrative(request.getNarrative());
        }

        sar = sarRepository.save(sar);
        return sarMapper.toResponse(sar);
    }

    @Transactional
    public SarResponse fileSar(Long id) {
        SuspiciousActivityReport sar = sarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SAR not found: " + id));

        if (sar.getStatus() == SarStatus.FILED) {
            throw new IllegalStateException("SAR is already filed");
        }

        sar.setStatus(SarStatus.FILED);
        sar.setFiledAt(LocalDateTime.now());

        // Simulating submission to FinCEN
        log.info("Filing SAR {} to FinCEN...", id);

        return mapToResponse(sarRepository.save(sar));
    }

    public String generateFincenXml(Long id) {
        SuspiciousActivityReport sar = sarRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SAR not found: " + id));

        // Simplified mock XML generation
        return String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<SAR>\n" +
                        "  <ActivityReportID>%d</ActivityReportID>\n" +
                        "  <FilingDate>%s</FilingDate>\n" +
                        "  <Narrative><![CDATA[%s]]></Narrative>\n" +
                        "  <SignerIdentifier>%s</SignerIdentifier>\n" +
                        "</SAR>",
                sar.getId(),
                sar.getFiledAt() != null ? sar.getFiledAt().toString() : "",
                sar.getNarrative() != null ? sar.getNarrative() : "",
                UUID.randomUUID().toString());
    }

    private String toJson(Map<String, Object> map) {
        if (map == null)
            return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Error serializing JSON", e);
            return "{}";
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null)
            return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.error("Error deserializing JSON", e);
            return null;
        }
    }

}
