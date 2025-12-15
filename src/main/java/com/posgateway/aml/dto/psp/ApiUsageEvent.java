package com.posgateway.aml.dto.psp;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ApiUsageEvent {
    private Long pspId;
    private Long userId;
    private String endpoint;
    private String httpMethod;
    private Integer responseStatus;
    private Integer responseTimeMs;
    private String serviceType;
    private String requestId;
    private Long merchantId;
    private String ipAddress;
    private String userAgent;
    private String externalProvider;
    private java.math.BigDecimal externalCost;
    private Integer requestSizeBytes;
    private Integer responseSizeBytes;
    private String errorMessage;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
