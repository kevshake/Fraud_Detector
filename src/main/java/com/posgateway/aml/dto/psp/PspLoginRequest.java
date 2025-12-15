package com.posgateway.aml.dto.psp;

import lombok.Data;

@Data
public class PspLoginRequest {
    private String email;
    private String password;
}
