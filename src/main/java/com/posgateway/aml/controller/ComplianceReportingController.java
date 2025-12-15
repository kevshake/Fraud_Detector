package com.posgateway.aml.controller;

import com.posgateway.aml.dto.compliance.CreateSarRequest;
import com.posgateway.aml.dto.compliance.SarResponse;
import com.posgateway.aml.dto.compliance.UpdateSarRequest;
import com.posgateway.aml.service.compliance.ComplianceReportingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compliance/sar")
@RequiredArgsConstructor
@Slf4j
public class ComplianceReportingController {

    private final ComplianceReportingService reportingService;

    @PostMapping
    public ResponseEntity<SarResponse> createSar(@RequestBody CreateSarRequest request) {
        log.info("Create SAR request for merchant: {}", request.getMerchantId());
        return ResponseEntity.ok(reportingService.createSar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SarResponse> updateSar(@PathVariable Long id, @RequestBody UpdateSarRequest request) {
        log.info("Update SAR request: {}", id);
        return ResponseEntity.ok(reportingService.updateSar(id, request));
    }

    @PostMapping("/{id}/file")
    public ResponseEntity<SarResponse> fileSar(@PathVariable Long id) {
        log.info("File SAR request: {}", id);
        return ResponseEntity.ok(reportingService.fileSar(id));
    }

    @GetMapping("/{id}/xml")
    public ResponseEntity<String> downloadSarXml(@PathVariable Long id) {
        log.info("Download SAR XML: {}", id);
        String xml = reportingService.generateFincenXml(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sar_" + id + ".xml\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}
