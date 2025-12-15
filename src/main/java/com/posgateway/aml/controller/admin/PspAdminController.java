package com.posgateway.aml.controller.admin;

import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.model.Permission;

import com.posgateway.aml.repository.PspRepository;
import com.posgateway.aml.service.PermissionService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/psp")
@RequiredArgsConstructor
@Slf4j
public class PspAdminController {

    private final PspRepository pspRepository;
    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<Psp>> list() {
        return ResponseEntity.ok(pspRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Psp> create(@RequestBody CreatePspRequest req, @RequestHeader("X-User-Role") String role) {
        ensurePspAdmin(role);
        if (pspRepository.existsByPspCode(req.getPspCode())) {
            throw new IllegalArgumentException("PSP code already exists");
        }
        Psp psp = Psp.builder()
                .pspCode(req.getPspCode())
                .legalName(req.getLegalName())
                .tradingName(req.getTradingName())
                .country(req.getCountry())
                .contactEmail(req.getContactEmail())
                .contactPhone(req.getContactPhone())
                .billingPlan(req.getBillingPlan() != null ? req.getBillingPlan() : "PAY_AS_YOU_GO")
                .billingCycle(req.getBillingCycle() != null ? req.getBillingCycle() : "MONTHLY")
                .status("PENDING")
                .logoUrl(req.getLogoUrl())
                .primaryColor(req.getPrimaryColor())
                .secondaryColor(req.getSecondaryColor())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return ResponseEntity.ok(pspRepository.save(psp));
    }

    @PutMapping("/{pspId}/activate")
    public ResponseEntity<Psp> activate(@PathVariable Long pspId, @RequestHeader("X-User-Role") String role) {
        ensurePspAdmin(role);
        Psp psp = pspRepository.findById(pspId).orElseThrow(() -> new IllegalArgumentException("PSP not found"));
        psp.activate();
        return ResponseEntity.ok(pspRepository.save(psp));
    }

    @PutMapping("/{pspId}/suspend")
    public ResponseEntity<Psp> suspend(@PathVariable Long pspId, @RequestHeader("X-User-Role") String role) {
        ensurePspAdmin(role);
        Psp psp = pspRepository.findById(pspId).orElseThrow(() -> new IllegalArgumentException("PSP not found"));
        psp.suspend("manual");
        return ResponseEntity.ok(pspRepository.save(psp));
    }

    @PutMapping("/{pspId}/terminate")
    public ResponseEntity<Psp> terminate(@PathVariable Long pspId, @RequestHeader("X-User-Role") String role) {
        ensurePspAdmin(role);
        Psp psp = pspRepository.findById(pspId).orElseThrow(() -> new IllegalArgumentException("PSP not found"));
        psp.terminate();
        return ResponseEntity.ok(pspRepository.save(psp));
    }

    @PutMapping("/{pspId}/theme")
    public ResponseEntity<Psp> updateTheme(@PathVariable Long pspId,
            @RequestBody ThemeRequest req,
            @RequestHeader("X-User-Role") String role) {
        ensurePspAdmin(role);
        Psp psp = pspRepository.findById(pspId).orElseThrow(() -> new IllegalArgumentException("PSP not found"));
        psp.setLogoUrl(req.getLogoUrl());
        psp.setPrimaryColor(req.getPrimaryColor());
        psp.setSecondaryColor(req.getSecondaryColor());
        psp.setAccentColor(req.getAccentColor());
        psp.setFontFamily(req.getFontFamily());
        psp.setFontSize(req.getFontSize());
        psp.setButtonRadius(req.getButtonRadius());
        psp.setButtonStyle(req.getButtonStyle());
        psp.setNavStyle(req.getNavStyle());
        psp.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(pspRepository.save(psp));
    }

    @DeleteMapping("/{pspId}")
    public ResponseEntity<Void> delete(@PathVariable Long pspId, @RequestHeader("X-User-Role") String role) {
        ensurePspAdmin(role);
        pspRepository.deleteById(pspId);
        return ResponseEntity.noContent().build();
    }

    private void ensurePspAdmin(String role) { // role param ignored now, preserved for signature compatibility if
                                               // needed, but safer to ignore.
        if (!permissionService.hasPermission(Permission.MANAGE_PSP)) {
            throw new SecurityException("Not authorized - requires MANAGE_PSP permission");
        }
    }

    @Data
    public static class CreatePspRequest {
        private String pspCode;
        private String legalName;
        private String tradingName;
        private String country;
        private String contactEmail;
        private String contactPhone;
        private String billingPlan;
        private String billingCycle;
        private String logoUrl;
        private String primaryColor;
        private String secondaryColor;
    }

    @Data
    public static class ThemeRequest {
        private String logoUrl;
        private String primaryColor;
        private String secondaryColor;
        private String accentColor;
        private String fontFamily;
        private String fontSize;
        private String buttonRadius;
        private String buttonStyle;
        private String navStyle;
    }
}
