package com.posgateway.aml.dto;

import java.util.List;

/**
 * Fraud Detection Response DTO
 * Response DTO for fraud detection results
 */
public class FraudDetectionResponseDTO {

    private Long txnId;
    private Double score;
    private String action;
    private List<String> reasons;
    private Long latencyMs;

    // Getters and Setters
    public Long getTxnId() {
        return txnId;
    }

    public void setTxnId(Long txnId) {
        this.txnId = txnId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }
}

