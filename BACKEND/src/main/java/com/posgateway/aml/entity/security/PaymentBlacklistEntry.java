package com.posgateway.aml.entity.security;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_blacklist_entries",
        uniqueConstraints = @UniqueConstraint(columnNames = {"entry_type", "entry_value"}))
public class PaymentBlacklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_type", nullable = false, length = 32)
    private String entryType;

    @Column(name = "entry_value", nullable = false, length = 255)
    private String entryValue;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }
    public String getEntryValue() { return entryValue; }
    public void setEntryValue(String entryValue) { this.entryValue = entryValue; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
