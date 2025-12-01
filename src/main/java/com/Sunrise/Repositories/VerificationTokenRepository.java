package com.Sunrise.Repositories;

import com.Sunrise.Entities.VerificationToken;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    @Query("SELECT vt FROM VerificationToken vt WHERE vt.token = :token")
    Optional<VerificationToken> findByToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken vt WHERE vt.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiryDate < :dateTime")
    int deleteByExpiryDateBefore(@Param("dateTime") LocalDateTime dateTime);
}
