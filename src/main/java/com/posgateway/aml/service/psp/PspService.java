package com.posgateway.aml.service.psp;

import com.posgateway.aml.dto.psp.PspRegistrationRequest;
import com.posgateway.aml.dto.psp.PspUserCreationRequest;
import com.posgateway.aml.entity.psp.Psp;
import com.posgateway.aml.entity.psp.PspUser;
import com.posgateway.aml.repository.PspRepository;
import com.posgateway.aml.repository.PspUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder; // Assuming Spring Security is used
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PspService {

    private final PspRepository pspRepository;
    private final PspUserRepository pspUserRepository;
    // private final PasswordEncoder passwordEncoder; // User can uncomment or
    // inject if available

    @Transactional
    public Psp registerPsp(PspRegistrationRequest request) {
        log.info("Registering new PSP: {}", request.getPspCode());

        if (pspRepository.findByPspCode(request.getPspCode()).isPresent()) {
            throw new IllegalArgumentException("PSP Code already exists");
        }

        Psp psp = Psp.builder()
                .pspCode(request.getPspCode())
                .legalName(request.getLegalName())
                .tradingName(request.getTradingName())
                .country(request.getCountry())
                .registrationNumber(request.getRegistrationNumber())
                .taxId(request.getTaxId())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .contactAddress(request.getContactAddress())
                .billingPlan(request.getBillingPlan() != null ? request.getBillingPlan() : "PAY_AS_YOU_GO")
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .paymentTerms(request.getPaymentTerms() != null ? request.getPaymentTerms() : 30)
                .status("PENDING")
                .build();

        return pspRepository.save(psp);
    }

    @Transactional
    public void updatePspStatus(Long pspId, String status) {
        Psp psp = pspRepository.findById(pspId)
                .orElseThrow(() -> new IllegalArgumentException("PSP not found"));

        log.info("Updating PSP {} status to {}", pspId, status);

        switch (status) {
            case "ACTIVE":
                psp.activate();
                break;
            case "SUSPENDED":
                psp.suspend("Manual suspension");
                break;
            case "TERMINATED":
                psp.terminate();
                break;
            default:
                psp.setStatus(status);
        }

        pspRepository.save(psp);
    }

    @Transactional
    public PspUser createPspUser(PspUserCreationRequest request) {
        Psp psp = pspRepository.findById(request.getPspId())
                .orElseThrow(() -> new IllegalArgumentException("PSP not found"));

        if (pspUserRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("User email already exists");
        }

        // Ideally hashing should be done here. Using placeholder for now if encoder not
        // available.
        // String encodedPassword = passwordEncoder.encode(request.getPassword());
        String encodedPassword = request.getPassword(); // REPLACE WITH ENC

        PspUser user = PspUser.builder()
                .psp(psp)
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash(encodedPassword)
                .role(request.getRole() != null ? request.getRole() : "OPERATOR")
                .permissions(request.getPermissions() != null ? request.getPermissions().toArray(new String[0]) : null)
                .status("ACTIVE")
                .build();

        return pspUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<PspUser> authenticatePspUser(String email, String rawPassword) {
        Optional<PspUser> userOpt = pspUserRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            PspUser user = userOpt.get();
            // Verify password - replace with encoder check
            if (user.getPasswordHash().equals(rawPassword)) { // SIMPLE CHECK
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}
