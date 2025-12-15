package com.posgateway.aml.dto.psp;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PspUserCreationRequest {
    private Long pspId;
    private String email;
    private String fullName;
    private String password; // Raw password
    private String role;
    private List<String> permissions;
}
