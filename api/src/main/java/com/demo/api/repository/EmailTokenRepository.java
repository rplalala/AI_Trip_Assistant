package com.demo.api.repository;

import com.demo.api.model.EmailToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByVerificationTokenAndUsedIsFalseAndExpireTimeAfter(
            String verificationToken, Instant now);
    Optional<EmailToken> findByResetPasswordTokenAndUsedIsFalseAndExpireTimeAfter(
            String resetPasswordToken, Instant now);
    void deleteByUserIdAndVerificationTokenIsNotNullAndUsedIsFalse(Long userId);
    void deleteByUserIdAndResetPasswordTokenIsNotNullAndUsedIsFalse(Long userId);
    long deleteByExpireTimeBefore(Instant now);

}
