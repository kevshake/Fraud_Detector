package com.posgateway.aml.service;

import com.posgateway.aml.entity.AuditLog;
import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.entity.compliance.SuspiciousActivityReport;
import com.posgateway.aml.model.CaseStatus;
import com.posgateway.aml.model.SarStatus;
import com.posgateway.aml.repository.AuditLogRepository;
import com.posgateway.aml.repository.ComplianceCaseRepository;
import com.posgateway.aml.repository.SuspiciousActivityReportRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private final ComplianceCaseRepository caseRepository;
    private final SuspiciousActivityReportRepository sarRepository;
    private final AuditLogRepository auditLogRepository;

    public ReportingService(ComplianceCaseRepository caseRepository,
                            SuspiciousActivityReportRepository sarRepository,
                            AuditLogRepository auditLogRepository) {
        this.caseRepository = caseRepository;
        this.sarRepository = sarRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Map<String, Object> summary() {
        Map<String, Object> result = new HashMap<>();
        result.put("casesByStatus", casesByStatus());
        result.put("sarsByStatus", sarsByStatus());
        result.put("auditLast24h", auditCountLastHours(24));
        result.put("casesLast7d", dailyCountsCases(7));
        result.put("sarsLast7d", dailyCountsSars(7));
        return result;
    }

    public Map<String, Long> auditHourly(int hoursBack) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(hoursBack);
        List<AuditLog> logs = auditLogRepository.findByTimestampBetween(start, now);
        Map<String, Long> buckets = new LinkedHashMap<>();
        for (int i = hoursBack - 1; i >= 0; i--) {
            LocalDateTime bucketStart = now.minusHours(i + 1);
            LocalDateTime bucketEnd = now.minusHours(i);
            long count = logs.stream()
                    .filter(l -> !l.getTimestamp().isBefore(bucketStart) && l.getTimestamp().isBefore(bucketEnd))
                    .count();
            buckets.put(bucketStart.toLocalTime().withMinute(0).withSecond(0).withNano(0).toString(), count);
        }
        return buckets;
    }

    public Map<CaseStatus, Long> casesByStatus() {
        Map<CaseStatus, Long> map = new EnumMap<>(CaseStatus.class);
        for (CaseStatus status : CaseStatus.values()) {
            map.put(status, caseRepository.countByStatus(status));
        }
        return map;
    }

    public Map<String, Long> casesStatusPriorityMatrix() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (CaseStatus s : CaseStatus.values()) {
            for (com.posgateway.aml.model.CasePriority p : com.posgateway.aml.model.CasePriority.values()) {
                long c = caseRepository.countByStatusAndPriority(s, p);
                result.put(s.name() + "|" + p.name(), c);
            }
        }
        return result;
    }

    public Map<SarStatus, Long> sarsByStatus() {
        Map<SarStatus, Long> map = new EnumMap<>(SarStatus.class);
        for (SarStatus status : SarStatus.values()) {
            map.put(status, sarRepository.countByStatus(status));
        }
        return map;
    }

    public Map<String, Long> dailyCountsCases(int daysBack) {
        return dailyCountsCases(daysBack, null);
    }

    public Map<String, Long> dailyCountsCases(int daysBack, String merchantId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(daysBack - 1).atStartOfDay();
        List<ComplianceCase> cases;
        if (merchantId != null && !merchantId.isEmpty()) {
            cases = caseRepository.findByCreatedAtBetween(start, LocalDateTime.now()).stream()
                    .filter(c -> merchantId.equals(c.getMerchantId()))
                    .toList();
        } else {
            cases = caseRepository.findByCreatedAtBetween(start, LocalDateTime.now());
        }
        return cases.stream()
                .collect(Collectors.groupingBy(c -> c.getCreatedAt().toLocalDate().toString(), Collectors.counting()));
    }

    public Map<String, Long> dailyCountsSars(int daysBack) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(daysBack - 1).atStartOfDay();
        List<SuspiciousActivityReport> sars = sarRepository.findByCreatedAtBetween(start, LocalDateTime.now());
        return sars.stream()
                .collect(Collectors.groupingBy(s -> s.getCreatedAt().toLocalDate().toString(), Collectors.counting()));
    }

    public long auditCountLastHours(int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(hours);
        return auditLogRepository.findByTimestampBetween(start, now).size();
    }
}

