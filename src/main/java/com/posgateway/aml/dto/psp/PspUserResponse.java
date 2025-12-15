package com.posgateway.aml.dto.psp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PspUserResponse {
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String status;
}
