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
}

