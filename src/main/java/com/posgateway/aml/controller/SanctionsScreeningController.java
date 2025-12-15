package com.posgateway.aml.controller;

import com.posgateway.aml.model.ScreeningResult;
import com.posgateway.aml.service.aml.AerospikeSanctionsScreeningService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for direct sanctions screening
 */
@RestController
@RequestMapping("/sanctions")
@Slf4j
public class SanctionsScreeningController {

    @Autowired
    private AerospikeSanctionsScreeningService screeningService;

    /**
     * Screen a name against sanctions database
     * POST /api/v1/sanctions/screen
     */
    @PostMapping("/screen")
    public ResponseEntity<ScreeningResult> screenName(@RequestBody ScreeningRequest request) {
        log.info("Screening name: {}", request.getName());

        try {
            ScreeningResult result = screeningService.screenName(
                    request.getName(),
                    request.getEntityType() != null ? ScreeningResult.EntityType.valueOf(request.getEntityType())
                            : ScreeningResult.EntityType.PERSON);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error screening name: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Screen a person (with DOB)
     * POST /api/v1/sanctions/screen/person
     */
    @PostMapping("/screen/person")
    public ResponseEntity<ScreeningResult> screenPerson(@RequestBody PersonScreeningRequest request) {
        log.info("Screening person: {}", request.getFullName());

        try {
            ScreeningResult result = screeningService.screenBeneficialOwner(
                    request.getFullName(),
                    request.getDateOfBirth());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error screening person: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Screen an organization
     * POST /api/v1/sanctions/screen/organization
     */
    @PostMapping("/screen/organization")
    public ResponseEntity<ScreeningResult> screenOrganization(@RequestBody OrganizationScreeningRequest request) {
        log.info("Screening organization: {}", request.getLegalName());

        try {
            ScreeningResult result = screeningService.screenMerchant(
                    request.getLegalName(),
                    request.getTradingName());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error screening organization: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Sanctions screening service is healthy");
    }

    @Data
    private static class ScreeningRequest {
        private String name;
        private String entityType; // PERSON, ORGANIZATION, VESSEL
    }

    @Data
    private static class PersonScreeningRequest {
        private String fullName;
        private LocalDate dateOfBirth;
    }

    @Data
    private static class OrganizationScreeningRequest {
        private String legalName;
        private String tradingName;
    }
}
