package com.posgateway.aml.controller.reporting;

import com.posgateway.aml.service.reporting.RegulatoryReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Regulatory Reporting Controller
 * Provides endpoints for CTR, LCTR, IFTR generation
 */
@RestController
@RequestMapping("/reporting/regulatory")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'MLRO')")
public class RegulatoryReportingController {

    private final RegulatoryReportingService regulatoryReportingService;

    @Autowired
    public RegulatoryReportingController(RegulatoryReportingService regulatoryReportingService) {
        this.regulatoryReportingService = regulatoryReportingService;
    }

    @GetMapping("/ctr")
    public ResponseEntity<RegulatoryReportingService.CurrencyTransactionReport> generateCtr(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusMonths(1);
        if (endDate == null) endDate = LocalDateTime.now();
        return ResponseEntity.ok(regulatoryReportingService.generateCtr(startDate, endDate));
    }

    @GetMapping("/lctr")
    public ResponseEntity<RegulatoryReportingService.LargeCashTransactionReport> generateLctr(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusMonths(1);
        if (endDate == null) endDate = LocalDateTime.now();
        return ResponseEntity.ok(regulatoryReportingService.generateLctr(startDate, endDate));
    }

    @GetMapping("/iftr")
    public ResponseEntity<RegulatoryReportingService.InternationalFundsTransferReport> generateIftr(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        if (startDate == null) startDate = LocalDateTime.now().minusMonths(1);
        if (endDate == null) endDate = LocalDateTime.now();
        return ResponseEntity.ok(regulatoryReportingService.generateIftr(startDate, endDate));
    }
}

