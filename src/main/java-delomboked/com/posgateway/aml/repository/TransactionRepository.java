package com.posgateway.aml.repository;

import com.posgateway.aml.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Transaction Entity
 */
@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    /**
     * Find transactions by merchant ID
     */
    List<TransactionEntity> findByMerchantId(String merchantId);

    /**
     * Find transactions by PAN hash
     */
    List<TransactionEntity> findByPanHash(String panHash);

    /**
     * Count transactions by merchant in time window
     */
    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.merchantId = :merchantId AND t.txnTs >= :startTime AND t.txnTs <= :endTime")
    Long countByMerchantInTimeWindow(@Param("merchantId") String merchantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Sum transaction amounts by merchant in time window
     */
    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM TransactionEntity t WHERE t.merchantId = :merchantId AND t.txnTs >= :startTime AND t.txnTs <= :endTime")
    Long sumAmountByMerchantInTimeWindow(@Param("merchantId") String merchantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count transactions by PAN in time window
     */
    @Query("SELECT COUNT(t) FROM TransactionEntity t WHERE t.panHash = :panHash AND t.txnTs >= :startTime AND t.txnTs <= :endTime")
    Long countByPanInTimeWindow(@Param("panHash") String panHash,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Sum transaction amounts by PAN in time window
     */
    @Query("SELECT COALESCE(SUM(t.amountCents), 0) FROM TransactionEntity t WHERE t.panHash = :panHash AND t.txnTs >= :startTime AND t.txnTs <= :endTime")
    Long sumAmountByPanInTimeWindow(@Param("panHash") String panHash,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find distinct terminals for PAN in time window
     */
    @Query("SELECT COUNT(DISTINCT t.terminalId) FROM TransactionEntity t WHERE t.panHash = :panHash AND t.txnTs >= :startTime AND t.txnTs <= :endTime")
    Long countDistinctTerminalsByPan(@Param("panHash") String panHash,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Get average amount by PAN in time window
     */
    @Query("SELECT COALESCE(AVG(t.amountCents), 0) FROM TransactionEntity t WHERE t.panHash = :panHash AND t.txnTs >= :startTime AND t.txnTs <= :endTime")
    Double avgAmountByPanInTimeWindow(@Param("panHash") String panHash,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find last transaction timestamp for PAN
     */
    @Query("SELECT MAX(t.txnTs) FROM TransactionEntity t WHERE t.panHash = :panHash")
    LocalDateTime findLastTransactionTimeByPan(@Param("panHash") String panHash);

    /**
     * Find distinct merchant IDs by Device Fingerprint
     */
    @Query("SELECT DISTINCT t.merchantId FROM TransactionEntity t WHERE t.deviceFingerprint = :deviceFingerprint")
    List<String> findMerchantIdsByDeviceFingerprint(@Param("deviceFingerprint") String deviceFingerprint);

    /**
     * Find distinct merchant IDs by IP Address
     */
    @Query("SELECT DISTINCT t.merchantId FROM TransactionEntity t WHERE t.ipAddress = :ipAddress")
    List<String> findMerchantIdsByIpAddress(@Param("ipAddress") String ipAddress);
}
