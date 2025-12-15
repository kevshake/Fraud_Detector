package com.posgateway.aml.dto.compliance;

import lombok.Data;
import java.util.Map;

@Data
public class CreateSarRequest {
    private Long merchantId;
    private Long caseId;
    private String priority;
    private String narrative;
    private Map<String, Object> suspectInfo;
    private Map<String, Object> activityInfo;
}
