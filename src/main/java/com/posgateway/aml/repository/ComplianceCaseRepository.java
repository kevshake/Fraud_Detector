package com.posgateway.aml.repository;

import com.posgateway.aml.entity.compliance.ComplianceCase;
import com.posgateway.aml.model.CasePriority;
import com.posgateway.aml.model.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Compliance Cases
 */
@Repository
public interface ComplianceCaseRepository extends JpaRepository<ComplianceCase, Long> {

    /**
     * Find cases by status
     */
    List<ComplianceCase> findByStatus(CaseStatus status);

    /**
     * Find cases by merchant
     */
    List<ComplianceCase> findByMerchantId(String merchantId);

    /**
     * Find cases assigned to user
     */
    List<ComplianceCase> findByAssignedTo_Id(Long userId);

    /**
     * Find open cases by priority
     */
    List<ComplianceCase> findByStatusAndPriorityOrderByCreatedAtAsc(CaseStatus status, CasePriority priority);

    /**
     * Count open cases by status
     */
    long countByStatus(CaseStatus status);

    /**
     * Count by priority
     */
    long countByPriority(CasePriority priority);

    /**
     * Count by status and priority
     */
    long countByStatusAndPriority(CaseStatus status, CasePriority priority);

    /**
     * Count by merchant and status
     */
    long countByMerchantIdAndStatus(String merchantId, CaseStatus status);

    /**
     * Count by merchant and priority
     */
    long countByMerchantIdAndPriority(String merchantId, CasePriority priority);

    /**
     * Find overdue cases
     */
    List<ComplianceCase> findBySlaDeadlineBeforeAndStatusNot(LocalDateTime date, CaseStatus status);

    /**
     * Find cases created within a window (for reporting)
     */
    List<ComplianceCase> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
