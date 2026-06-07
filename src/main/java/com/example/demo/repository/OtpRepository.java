package com.example.demo.repository;

import com.example.demo.model.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findByCodeAndOperationIdAndStatus(String code, String operationId, OtpCode.OtpStatus status);

    List<OtpCode> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE OtpCode o SET o.status = 'EXPIRED' WHERE o.status = 'ACTIVE' AND o.expiresAt < :now")
    int expireOldCodes(LocalDateTime now);
}
