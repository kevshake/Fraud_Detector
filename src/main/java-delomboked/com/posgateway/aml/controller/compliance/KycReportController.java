package com.posgateway.aml.controller.compliance;

import com.posgateway.aml.service.analytics.CorporateStructureService;
import com.posgateway.aml.service.analytics.CorporateStructureService.CorporateGraph;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compliance/kyc")
public class KycReportController {

    private final CorporateStructureService corporateStructureService;

    /**
     * Get Corporate Structure Graph
     * Returns nodes and edges representing UBOs and related companies
     */
    @GetMapping("/corporate-graph/{merchantId}")
    public ResponseEntity<CorporateGraph> getCorporateGraph(@PathVariable Long merchantId) {
        log.info("Requesting Corporate Graph for Merchant ID: {}", merchantId);
        try {
            CorporateGraph graph = corporateStructureService.buildCorporateGraph(merchantId);
            return ResponseEntity.ok(graph);
        } catch (IllegalArgumentException e) {
            log.warn("Merchant not found: {}", merchantId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error generating graph", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
