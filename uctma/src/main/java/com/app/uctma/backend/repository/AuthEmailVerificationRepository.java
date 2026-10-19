package com.app.uctma.backend.repository;

import com.app.uctma.backend.model.AuthEmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface AuthEmailVerificationRepository extends JpaRepository<AuthEmailVerification, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select verification from AuthEmailVerification verification where verification.user.id = :userId")
    Optional<AuthEmailVerification> findByUserIdForUpdate(@Param("userId") Long userId);

    Optional<AuthEmailVerification> findByUserId(Long userId);
}
