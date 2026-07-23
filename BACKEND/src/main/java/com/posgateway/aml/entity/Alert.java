package com.posgateway.aml.entity;

import com.posgateway.aml.model.AlertDisposition;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Alert Entity
 * Stores generated alerts and cases for manual review
 */
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alert_status", columnList = "status"),
        @Index(name = "idx_alert_created", columnList = "created_at"),
        @Index(name = "idx_alert_txn", columnList = "txn_id")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long alertId;

    @Column(name = "txn_id")
    private Long txnId;

    @Column(name = "score")
    private Double score;

    @Column(name = "action")
    private String action;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "status", nullable = false)
    private String status = "open"; // open, closed, false_positive

    @Column(name = "investigator")
    private String investigator;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "psp_id")
    private Long pspId;

    @Column(name = "multi_asset_customer_id")
    private Long multiAssetCustomerId;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "source_reference")
    private String sourceReference;

    @Column(name = "severity")
    private String severity; // INFO, WARN, CRITICAL

    @Column(name = "sar_required", nullable = false)
    private boolean sarRequired;

    @Column(name = "ctr_required", nullable = false)
    private boolean ctrRequired;

    @Column(name = "triggered_rules", columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Alert Disposition Fields
    @Enumerated(EnumType.STRING)
    @Column(name = "disposition")
    private AlertDisposition disposition;

    @Column(name = "disposition_reason", columnDefinition = "TEXT")
    private String dispositionReason;

    @Column(name = "disposed_by")
    private String disposedBy; // Investigator who disposed the alert

    @Column(name = "disposed_at")
    private LocalDateTime disposedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInvestigator() {
        return investigator;
    }

    public void setInvestigator(String investigator) {
        this.investigator = investigator;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public Long getPspId() { return pspId; }
    public void setPspId(Long pspId) { this.pspId = pspId; }
    public Long getMultiAssetCustomerId() { return multiAssetCustomerId; }
    public void setMultiAssetCustomerId(Long multiAssetCustomerId) { this.multiAssetCustomerId = multiAssetCustomerId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public boolean isSarRequired() { return sarRequired; }
    public void setSarRequired(boolean sarRequired) { this.sarRequired = sarRequired; }
    public boolean isCtrRequired() { return ctrRequired; }
    public void setCtrRequired(boolean ctrRequired) { this.ctrRequired = ctrRequired; }
    public String getTriggeredRules() { return triggeredRules; }
    public void setTriggeredRules(String triggeredRules) { this.triggeredRules = triggeredRules; }

    public AlertDisposition getDisposition() {
        return disposition;
    }

    public void setDisposition(AlertDisposition disposition) {
        this.disposition = disposition;
    }

    public String getDispositionReason() {
        return dispositionReason;
    }

    public void setDispositionReason(String dispositionReason) {
        this.dispositionReason = dispositionReason;
    }

    public String getDisposedBy() {
        return disposedBy;
    }

    public void setDisposedBy(String disposedBy) {
        this.disposedBy = disposedBy;
    }

    public LocalDateTime getDisposedAt() {
        return disposedAt;
    }

    public void setDisposedAt(LocalDateTime disposedAt) {
        this.disposedAt = disposedAt;
    }
}
