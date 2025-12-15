package com.posgateway.aml.repository;

import com.posgateway.aml.entity.psp.PspUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PSP User Repository
 */
@Repository
public interface PspUserRepository extends JpaRepository<PspUser, Long> {

    Optional<PspUser> findByEmail(String email);

    List<PspUser> findByPsp_PspId(Long pspId);

    List<PspUser> findByPsp_PspIdAndStatus(Long pspId, String status);

    @Query("SELECT u FROM PspUser u WHERE u.psp.pspId = :pspId AND u.role = :role")
    List<PspUser> findByPspAndRole(@Param("pspId") Long pspId, @Param("role") String role);

    @Query("SELECT COUNT(u) FROM PspUser u WHERE u.psp.pspId = :pspId")
    long countByPsp(@Param("pspId") Long pspId);

    boolean existsByEmail(String email);
}
