package com.posgateway.aml.controller;



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.posgateway.aml.dto.compliance.CreateSarRequest;
import com.posgateway.aml.dto.compliance.SarResponse;
import com.posgateway.aml.dto.compliance.UpdateSarRequest;
import com.posgateway.aml.service.compliance.ComplianceReportingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// @Slf4j removed
// @RequiredArgsConstructor removed
@RestController
@RequestMapping("/api/v1/compliance/sar")
public class ComplianceReportingController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceReportingController.class);

    private final ComplianceReportingService reportingService;

    public ComplianceReportingController(ComplianceReportingService reportingService) {
        this.reportingService = reportingService;
    }


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
