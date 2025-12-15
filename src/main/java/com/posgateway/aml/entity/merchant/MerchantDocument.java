package com.posgateway.aml.entity.merchant;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "merchant_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    @Column(nullable = false)
    private Long merchantId;

    @Column(nullable = false)
    private String documentType; // e.g., PASSPORT, LICENSE, UTILITY_BILL

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String status; // PENDING, VERIFIED, REJECTED

    private java.time.LocalDate expiryDate;

    private LocalDateTime uploadedAt;
    private LocalDateTime verifiedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}
