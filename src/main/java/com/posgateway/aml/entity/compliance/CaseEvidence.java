package com.posgateway.aml.entity.compliance;

import com.posgateway.aml.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Case Evidence Entity
 * Represents a file or document attached to a compliance case
 */
@Entity
@Table(name = "case_evidence")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private ComplianceCase complianceCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private String fileName;
    
    @Column(nullable = false)
    private String fileType; // PDF, JPG, etc.
    
    @Column(nullable = false)
    private String storagePath; // Path or URL to stored file
    
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}

