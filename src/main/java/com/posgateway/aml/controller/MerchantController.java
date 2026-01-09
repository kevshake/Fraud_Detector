package com.posgateway.aml.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.posgateway.aml.dto.request.MerchantOnboardingRequest;
import com.posgateway.aml.dto.request.MerchantUpdateRequest;
import com.posgateway.aml.dto.response.MerchantOnboardingResponse;
import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.repository.MerchantRepository;
import com.posgateway.aml.repository.PspRepository;
import com.posgateway.aml.service.merchant.MerchantOnboardingService;
import com.posgateway.aml.service.merchant.MerchantUpdateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for merchant management
 * 
 * Security: Role-based access for merchant operations
 */
// @Slf4j removed
@RestController
@RequestMapping("/merchants")
@PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'SCREENING_ANALYST')")
public class MerchantController {

    private static final Logger log = LoggerFactory.getLogger(MerchantController.class);

    @Autowired
    private MerchantOnboardingService onboardingService;

    @Autowired
    private MerchantUpdateService updateService;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PspRepository pspRepository;

    /**
     * Onboard new merchant
     * POST /api/v1/merchants/onboard
     */
    @PostMapping("/onboard")
    @PreAuthorize("hasAuthority('ONBOARD_MERCHANT')")
    public ResponseEntity<MerchantOnboardingResponse> onboardMerchant(
            @Valid @RequestBody MerchantOnboardingRequest request) {

        log.info("Received merchant onboarding request for: {}", request.getLegalName());

        try {
            MerchantOnboardingResponse response = onboardingService.onboardMerchant(request);

            HttpStatus status = switch (response.getDecision()) {
                case "APPROVE" -> HttpStatus.CREATED; // 201
                case "REVIEW" -> HttpStatus.ACCEPTED; // 202
                case "REJECT" -> HttpStatus.FORBIDDEN; // 403
                default -> HttpStatus.OK;
            };

            return ResponseEntity.status(status).body(response);

        } catch (Exception e) {
            log.error("Error onboarding merchant: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create simplified merchant (Quick Add)
     * POST /api/v1/merchants
     */
    @PostMapping
    public ResponseEntity<Merchant> createMerchant(@RequestBody java.util.Map<String, Object> data) {
        log.info("Creating new merchant via quick add: {}", data.get("legalName"));
        try {
            // Find default PSP - using first available for now
            com.posgateway.aml.entity.psp.Psp defaultPsp = pspRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RuntimeException("No PSP found in system"));

            Merchant merchant = Merchant.builder()
                    .legalName((String) data.get("legalName"))
                    .tradingName((String) data.get("tradingName"))
                    .contactEmail((String) data.get("contactEmail"))
                    .mcc((String) data.get("mcc"))
                    .businessType((String) data.get("businessType"))
                    .country("KEN") // Default to Kenya for quick add
                    .registrationNumber("TEMP-" + java.util.UUID.randomUUID().toString().substring(0, 8))
                    .status("ACTIVE")
                    .psp(defaultPsp)
                    .kycStatus("APPROVED") // Auto-approve for quick add
                    .contractStatus("ACTIVE")
                    .dailyLimit(data.get("dailyLimit") != null
                            ? new java.math.BigDecimal(data.get("dailyLimit").toString())
                            : java.math.BigDecimal.valueOf(100000))
                    .riskLevel(data.get("riskLevel") != null ? (String) data.get("riskLevel") : "LOW")
                    .build();

            Merchant saved = merchantRepository.save(merchant);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Error creating merchant: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all merchants
     * GET /api/v1/merchants
     */
    @GetMapping
    public ResponseEntity<List<MerchantOnboardingResponse>> getAllMerchants() {
        log.info("Get all merchants request");
        try {
            List<Merchant> merchants = merchantRepository.findAll();
            List<MerchantOnboardingResponse> responses = merchants.stream()
                    .filter(m -> m.getMerchantId() != null) // Filter out null IDs
                    .map(m -> {
                        try {
                            return onboardingService.getMerchantById(m.getMerchantId());
                        } catch (Exception e) {
                            log.warn("Error fetching merchant {}: {}", m.getMerchantId(), e.getMessage());
                            // Return a minimal response for failed merchants
                            return MerchantOnboardingResponse.builder()
                                    .merchantId(m.getMerchantId())
                                    .legalName(m.getLegalName() != null ? m.getLegalName() : "Unknown")
                                    .tradingName(m.getTradingName())
                                    .contactEmail(m.getContactEmail())
                                    .mcc(m.getMcc())
                                    .businessType(m.getBusinessType())
                                    .status(m.getStatus() != null ? m.getStatus() : "PENDING_SCREENING")
                                    .country(m.getCountry())
                                    .kycStatus(m.getKycStatus() != null ? m.getKycStatus() : "PENDING")
                                    .contractStatus(
                                            m.getContractStatus() != null ? m.getContractStatus() : "NO_CONTRACT")
                                    .dailyLimit(m.getDailyLimit())
                                    .currentUsage(m.getCurrentUsage())
                                    .riskLevel(m.getRiskLevel() != null ? m.getRiskLevel() : "UNKNOWN")
                                    .mccDescription(m.getMcc() != null ? "Unknown Category" : null)
                                    .build();
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            log.error("Error fetching merchants: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get merchant by ID
     * GET /api/v1/merchants/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'SCREENING_ANALYST', 'PSP_ADMIN', 'PSP_USER')")
    public ResponseEntity<MerchantOnboardingResponse> getMerchant(@PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        log.info("Get merchant request for ID: {} by user: {}", id, authentication.getName());
        try {
            // Ideally: Check if user has access to this merchant's PSP
            MerchantOnboardingResponse response = onboardingService.getMerchantById(id);
            // Validation logic should be in service or here:
            // if (currentUser.psp != null &&
            // !currentUser.psp.id.equals(response.getPspId())) throw Forbidden
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Merchant not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update merchant
     * PUT /api/v1/merchants/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Merchant> updateMerchant(@PathVariable Long id,
            @Valid @RequestBody MerchantUpdateRequest request) {
        log.info("Update merchant request for ID: {}", id);
        try {
            Merchant merchant = updateService.updateMerchant(id, request);
            return ResponseEntity.ok(merchant);
        } catch (IllegalArgumentException e) {
            log.warn("Merchant not found for update: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating merchant: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete merchant
     * DELETE /api/v1/merchants/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMerchant(@PathVariable Long id) {
        log.info("Delete merchant request for ID: {}", id);
        try {
            updateService.deleteMerchant(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error deleting merchant: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check
     * GET /api/v1/merchants/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Merchant service is healthy");
    }
}
