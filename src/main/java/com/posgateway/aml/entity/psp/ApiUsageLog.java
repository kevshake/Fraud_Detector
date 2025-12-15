package com.posgateway.aml.entity.psp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API Usage Log Entity
 * Tracks all API requests for billing and analytics
 */
@Entity
@Table(name = "api_usage_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "psp_id", nullable = false)
    private Psp psp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private PspUser user;

    // Request Details
    @Column(name = "endpoint", nullable = false, length = 500)
    private String endpoint;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_timestamp")
    @Builder.Default
    private LocalDateTime requestTimestamp = LocalDateTime.now();

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    // Usage Categorization
    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Column(name = "billable")
    @Builder.Default
    private Boolean billable = true;

    @Column(name = "cost_amount", precision = 10, scale = 4)
    private BigDecimal costAmount;

    @Column(name = "cost_currency", length = 3)
    @Builder.Default
    private String costCurrency = "USD";

    // Request Metadata
    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    // External Costs
    @Column(name = "external_provider", length = 100)
    private String externalProvider;

    @Column(name = "external_cost", precision = 10, scale = 4)
    private BigDecimal externalCost;

    // Additional Data
    @Column(name = "request_size_bytes")
    private Integer requestSizeBytes;

    @Column(name = "response_size_bytes")
    private Integer responseSizeBytes;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
}
