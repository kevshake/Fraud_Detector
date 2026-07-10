package com.posgateway.aml.repository.security;

import com.posgateway.aml.entity.security.PaymentBlacklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentBlacklistRepository extends JpaRepository<PaymentBlacklistEntry, Long> {

    Optional<PaymentBlacklistEntry> findByEntryTypeAndEntryValueAndActiveTrue(String entryType, String entryValue);

    boolean existsByEntryTypeAndEntryValueAndActiveTrue(String entryType, String entryValue);
}
