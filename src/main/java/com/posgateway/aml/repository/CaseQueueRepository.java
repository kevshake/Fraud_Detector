package com.posgateway.aml.repository;

import com.posgateway.aml.entity.compliance.CaseQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Case Queues
 */
@Repository
public interface CaseQueueRepository extends JpaRepository<CaseQueue, Long> {

    /**
     * Find queue by name
     */
    Optional<CaseQueue> findByQueueName(String queueName);

    /**
     * Find all enabled auto-assign queues
     */
    List<CaseQueue> findByAutoAssignTrueAndEnabledTrue();

    /**
     * Find all enabled queues
     */
    List<CaseQueue> findByEnabledTrue();
}

