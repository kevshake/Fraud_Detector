package com.posgateway.aml.service.edd;

import com.posgateway.aml.entity.merchant.Merchant;
import com.posgateway.aml.repository.MerchantRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedDueDiligenceService {

    private final MerchantRepository merchantRepository;

    // In-memory store for demo purposes. In prod, use a DB table 'edd_requests'
    private final Map<Long, EddStatus> eddTracking = new ConcurrentHashMap<>();

    public void initiateEdd(Long merchantId) {
        log.info("Initiating Enhanced Due Diligence for Merchant {}", merchantId);
        EddStatus status = new EddStatus();
        status.setMerchantId(merchantId);
        status.setSourceOfFundsVerified(false);
        status.setSourceOfWealthVerified(false);
        status.setSiteVisitCompleted(false);
        eddTracking.put(merchantId, status);
    }

    public void updateDocumentStatus(Long merchantId, String docType, boolean verified) {
        EddStatus status = eddTracking.get(merchantId);
        if (status != null) {
            switch (docType) {
                case "SOF" -> status.setSourceOfFundsVerified(verified);
                case "SOW" -> status.setSourceOfWealthVerified(verified);
                case "VISIT" -> status.setSiteVisitCompleted(verified);
                case "SENIOR_APPROVAL" -> status.setSeniorManagementApproval(verified);
                case "FAMILY_CHECK" -> status.setFamilyAssociateChecks(verified);
                case "PURPOSE_REVIEW" -> status.setTransactionPurposeReview(verified);
            }

            log.info("Updated EDD status for Merchant {}: {} = {}", merchantId, docType, verified);
            checkCompletion(status);
        }
    }

    public EddStatus getEddStatus(Long merchantId) {
        return eddTracking.getOrDefault(merchantId, new EddStatus());
    }

    private void checkCompletion(EddStatus status) {
        boolean basicEdd = status.isSourceOfFundsVerified() && status.isSourceOfWealthVerified();
        boolean kenyaEdd = status.isSeniorManagementApproval() && status.isFamilyAssociateChecks()
                && status.isTransactionPurposeReview();

        if (basicEdd && kenyaEdd) {
            log.info("EDD Completed for Merchant {}", status.getMerchantId());
            // Could trigger workflow to unblock merchant
        }
    }

    @Data
    public static class EddStatus {
        private Long merchantId;
        private boolean sourceOfFundsVerified;
        private boolean sourceOfWealthVerified;
        private boolean siteVisitCompleted;
        // Kenyan Specific (Phase 29)
        private boolean seniorManagementApproval;
        private boolean familyAssociateChecks;
        private boolean transactionPurposeReview;
    }
}
