package com.posgateway.aml.controller.psp;

import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.repository.PspRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * PSP read-only controller for UI to fetch theme and branding dynamically
 */
@RestController
@RequestMapping("/api/v1/psp")
@RequiredArgsConstructor
public class PspController {

    private final PspRepository pspRepository;

    /**
     * Get PSP by code (preferred for UI)
     */
    @GetMapping("/code/{pspCode}")
    public ResponseEntity<Psp> getByCode(@PathVariable String pspCode) {
        return ResponseEntity.of(pspRepository.findByPspCode(pspCode));
    }

    /**
     * Get PSP by id
     */
    @GetMapping("/{pspId}")
    public ResponseEntity<Psp> getById(@PathVariable Long pspId) {
        return ResponseEntity.of(pspRepository.findById(pspId));
    }

    /**
     * Get PSP by code passed as query param (e.g., from login context)
     */
    @GetMapping("/current")
    public ResponseEntity<Psp> getCurrent(@RequestParam("pspCode") String pspCode) {
        Optional<Psp> psp = pspRepository.findByPspCode(pspCode);
        return ResponseEntity.of(psp);
    }
}
package com.posgateway.aml.controller.psp;

import com.posgateway.aml.dto.psp.PspLoginRequest;
import com.posgateway.aml.dto.psp.PspRegistrationRequest;
import com.posgateway.aml.dto.psp.PspResponse;
import com.posgateway.aml.dto.psp.PspStatusUpdateRequest;
import com.posgateway.aml.dto.psp.PspUserCreationRequest;
import com.posgateway.aml.dto.psp.PspUserResponse;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.entity.psp.PspUser;
import com.posgateway.aml.service.psp.PspService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/psps")
@RequiredArgsConstructor
@Slf4j
public class PspController {

    private final PspService pspService;

    @PostMapping
    public ResponseEntity<PspResponse> registerPsp(@RequestBody PspRegistrationRequest request) {
        log.info("Received PSP registration request");
        Psp psp = pspService.registerPsp(request);
        return ResponseEntity.ok(mapToPspResponse(psp));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updatePspStatus(@PathVariable Long id, @RequestBody PspStatusUpdateRequest request) {
        log.info("Received status update for PSP {}", id);
        pspService.updatePspStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users")
    public ResponseEntity<PspUserResponse> createPspUser(@RequestBody PspUserCreationRequest request) {
        log.info("Received PSP user creation request");
        PspUser user = pspService.createPspUser(request);
        return ResponseEntity.ok(mapToPspUserResponse(user));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<PspUserResponse> login(@RequestBody PspLoginRequest request) {
        Optional<PspUser> userOpt = pspService.authenticatePspUser(request.getEmail(), request.getPassword());
        return userOpt.map(user -> ResponseEntity.ok(mapToPspUserResponse(user)))
                .orElse(ResponseEntity.status(401).build());
    }

    private PspResponse mapToPspResponse(Psp psp) {
        return PspResponse.builder()
                .id(psp.getPspId())
                .pspCode(psp.getPspCode())
                .legalName(psp.getLegalName())
                .status(psp.getStatus())
                .billingPlan(psp.getBillingPlan())
                .build();
    }

    private PspUserResponse mapToPspUserResponse(PspUser user) {
        return PspUserResponse.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
