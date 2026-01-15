package com.posgateway.aml.repository;

import com.posgateway.aml.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Audit Logs
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, String entityId);

    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}

