package com.posgateway.aml.controller.reporting;

import com.posgateway.aml.entity.reporting.ReportResult;
import com.posgateway.aml.repository.reporting.ReportResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reports/results")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MLRO', 'COMPLIANCE_OFFICER', 'PSP_ADMIN', 'ANALYST')")
public class ReportResultController {

    private final ReportResultRepository resultRepository;

    public ReportResultController(ReportResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    @GetMapping("/execution/{executionId}")
    public List<ReportResult> listByExecution(@PathVariable Long executionId) {
        return resultRepository.findByExecutionIdOrderByRowNumberAsc(executionId);
    }

    @GetMapping("/execution/{executionId}/page")
    public Page<ReportResult> pageByExecution(@PathVariable Long executionId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "100") int size) {
        return resultRepository.findByExecutionId(executionId, PageRequest.of(page, size));
    }

    @PostMapping("/execution/{executionId}")
    public ResponseEntity<ReportResult> saveRow(@PathVariable Long executionId,
                                                @RequestBody Map<String, Object> body) {
        ReportResult row = new ReportResult();
        row.setExecutionId(executionId);
        row.setRowNumber(Integer.valueOf(body.get("rowNumber").toString()));
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        row.setData(data);
        return ResponseEntity.ok(resultRepository.save(row));
    }
}
