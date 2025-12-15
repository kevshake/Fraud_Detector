package com.posgateway.aml.dto.psp;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PspUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
}
