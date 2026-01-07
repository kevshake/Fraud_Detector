package com.posgateway.aml.repository;

import com.posgateway.aml.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Alerts
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Find alerts by status
     */
    List<Alert> findByStatus(String status);

    /**
     * Find open alerts
     */
    @Query("SELECT a FROM Alert a WHERE a.status = 'open' ORDER BY a.createdAt DESC")
    List<Alert> findOpenAlerts();

    /**
     * Find alerts by transaction ID
     */
    List<Alert> findByTxnId(Long txnId);

    /**
     * Count alerts by status
     */
    Long countByStatus(String status);

    /**
     * Find alerts created in time range
     */
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :startTime AND a.createdAt <= :endTime")
    List<Alert> findAlertsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Find unassigned alerts
     */
    @Query("SELECT a FROM Alert a WHERE a.investigator IS NULL OR a.investigator = ''")
    List<Alert> findByAssignedToIsNull();

    /**
     * Find alerts created after a date
     */
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :afterDate")
    List<Alert> findByCreatedAtAfter(@Param("afterDate") LocalDateTime afterDate);

    /**
     * Find alerts created between dates
     */
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :startDate AND a.createdAt <= :endDate")
    List<Alert> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Find recent open alerts filtered by PSP ID (through merchant)
     * Returns alerts for merchants belonging to the specified PSP
     */
    @Query(value = "SELECT DISTINCT a.* FROM alerts a " +
            "INNER JOIN merchants m ON a.merchant_id = m.merchant_id " +
            "WHERE m.psp_id = :pspId AND a.status = 'open' " +
            "ORDER BY a.created_at DESC LIMIT :limit", nativeQuery = true)
    List<Alert> findRecentOpenAlertsByPspId(@Param("pspId") Long pspId, @Param("limit") int limit);

    /**
     * Find recent open alerts for all PSPs (admin view)
     * Limited to most recent alerts
     */
    @Query("SELECT a FROM Alert a WHERE a.status = 'open' ORDER BY a.createdAt DESC")
    List<Alert> findRecentOpenAlerts();
}

