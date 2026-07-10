package com.posgateway.aml.controller.reporting;

import com.posgateway.aml.service.reporting.RegulatoryReportingService;
import com.posgateway.aml.service.reporting.ReportExportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/reporting/regulatory")
public class RegulatoryReportingController {

    private static final Logger log = LoggerFactory.getLogger(RegulatoryReportingController.class);

    private final RegulatoryReportingService reportingService;
    private final ReportExportService reportExportService;

    public RegulatoryReportingController(RegulatoryReportingService reportingService,
                                          ReportExportService reportExportService) {
        this.reportingService = reportingService;
        this.reportExportService = reportExportService;
    }

    /** Generate Currency Transaction Report (CTR) — JSON */
    @GetMapping("/ctr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER', 'PSP_ADMIN', 'PSP_COMPLIANCE_OFFICER')")
    public ResponseEntity<RegulatoryReportingService.CurrencyTransactionReport> generateCtr(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();
        log.info("Generating CTR from {} to {}", startDate, endDate);
        return ResponseEntity.ok(reportingService.generateCtr(startDate, endDate));
    }

    /** Generate Large Cash Transaction Report (LCTR) — JSON */
    @GetMapping("/lctr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER', 'PSP_ADMIN', 'PSP_COMPLIANCE_OFFICER')")
    public ResponseEntity<RegulatoryReportingService.LargeCashTransactionReport> generateLctr(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();
        log.info("Generating LCTR from {} to {}", startDate, endDate);
        return ResponseEntity.ok(reportingService.generateLctr(startDate, endDate));
    }

    /** Generate International Funds Transfer Report (IFTR) — JSON */
    @GetMapping("/iftr")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMPLIANCE_OFFICER', 'PSP_ADMIN', 'PSP_COMPLIANCE_OFFICER')")
    public ResponseEntity<RegulatoryReportingService.InternationalFundsTransferReport> generateIftr(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();
        log.info("Generating IFTR from {} to {}", startDate, endDate);
        return ResponseEntity.ok(reportingService.generateIftr(startDate, endDate));
    }

    // =========================================================================
    // EXPORT: CTR / LCTR / IFTR as CSV or PDF
    // =========================================================================

    /**
     * Export a regulatory report in the requested format.
     *
     * @param type      report type: ctr, lctr, or iftr
     * @param format    export format: csv or pdf (default: csv)
     * @param startDate start of reporting window
     * @param endDate   end of reporting window
     */
    @GetMapping("/{type}/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'PSP_ADMIN')")
    public ResponseEntity<byte[]> exportReport(
            @PathVariable String type,
            @RequestParam(defaultValue = "csv") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        if (startDate == null) startDate = LocalDateTime.now().minusDays(30);
        if (endDate == null) endDate = LocalDateTime.now();

        List<Map<String, Object>> reportData;
        String reportName;

        switch (type.toLowerCase()) {
            case "ctr" -> {
                var report = reportingService.generateCtr(startDate, endDate);
                reportData = flattenReport(report.getTransactions(), "transactionId", "amount", "currency", "timestamp");
                reportName = "Currency_Transaction_Report";
            }
            case "lctr" -> {
                var report = reportingService.generateLctr(startDate, endDate);
                reportData = flattenReport(report.getTransactions(), "transactionId", "amount", "currency", "timestamp");
                reportName = "Large_Cash_Transaction_Report";
            }
            case "iftr" -> {
                            var report = reportingService.generateIftr(startDate, endDate);
                            reportData = flattenReport(report.getTransactions(), "transactionId", "amount", "currency", "transferType", "timestamp");
                            reportName = "International_Funds_Transfer_Report";
                        }
            default -> {
                return ResponseEntity.badRequest().build();
            }
        }

        byte[] content;
        String extension;
        MediaType mediaType;

        boolean isPdf = "pdf".equalsIgnoreCase(format);
        if (isPdf) {
            Map<String, String> meta = new LinkedHashMap<>();
            meta.put("Report Type", type.toUpperCase());
            meta.put("Period", startDate.toLocalDate() + " to " + endDate.toLocalDate());
            content = reportExportService.exportToPDF(reportData, reportName, meta);
            extension = "pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else {
            content = reportExportService.exportToCSV(reportData);
            extension = "csv";
            mediaType = new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8);
        }

        String fileName = reportName + "_" + startDate.toLocalDate() + "." + extension;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(content);
    }

    /** Flatten a list of domain objects into a list of maps for the report export service. */
    private List<Map<String, Object>> flattenReport(List<?> items, String... keys) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : items) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String key : keys) {
                try {
                    var getter = item.getClass().getMethod(
                            "get" + Character.toUpperCase(key.charAt(0)) + key.substring(1));
                    row.put(key, getter.invoke(item));
                } catch (Exception e) {
                    row.put(key, null);
                }
            }
            result.add(row);
        }
        return result;
    }
}
