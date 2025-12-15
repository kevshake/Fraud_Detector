package com.posgateway.aml.dto.compliance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class SarResponse {
    private Long reportId;
    private Long merchantId;
    private Long caseId;
    private String status;
    private String priority;
    private LocalDateTime filingDate;
    private String narrative;
    private Map<String, Object> suspectInfo;
    private Map<String, Object> activityInfo;
    private LocalDateTime createdAt;
    private String createdBy;
}
