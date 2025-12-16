package com.posgateway.aml.controller.psp;

import com.posgateway.aml.entity.User;
import com.posgateway.aml.dto.psp.PspLoginRequest;
import com.posgateway.aml.dto.psp.PspRegistrationRequest;
import com.posgateway.aml.dto.psp.PspResponse;
import com.posgateway.aml.dto.psp.PspStatusUpdateRequest;
import com.posgateway.aml.dto.psp.PspUserCreationRequest;
import com.posgateway.aml.dto.psp.PspUserResponse;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.mapper.PspMapper;
import com.posgateway.aml.service.psp.PspService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/psps")
public class PspController {

    private final PspService pspService;
    private final PspMapper pspMapper;

    @PostMapping
    public ResponseEntity<PspResponse> registerPsp(@RequestBody PspRegistrationRequest request) {
        log.info("Received PSP registration request");
        Psp psp = pspService.registerPsp(request);
        return ResponseEntity.ok(pspMapper.toResponse(psp));
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
        User user = pspService.createPspUser(request);
        return ResponseEntity.ok(pspMapper.toResponse(user));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<PspUserResponse> login(@RequestBody PspLoginRequest request) {
        Optional<User> userOpt = pspService.authenticatePspUser(request.getEmail(), request.getPassword());
        return userOpt.map(user -> ResponseEntity.ok(pspMapper.toResponse(user)))
                .orElse(ResponseEntity.status(401).build());
    }
}
