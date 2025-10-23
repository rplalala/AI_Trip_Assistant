package com.demo.api.service.impl;

import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.exception.AuthException;
import com.demo.api.exception.BusinessException;
import com.demo.api.model.EmailToken;
import com.demo.api.model.User;
import com.demo.api.repository.EmailTokenRepository;
import com.demo.api.repository.UserRepository;
import com.demo.api.service.AuthService;
import com.demo.api.utils.SendGridUtils;
import com.demo.api.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final SendGridUtils sendGridUtils;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Value( "${default.avatar-url}")
    private String DEFAULT_AVATAR; // Default avatar

    private final String baseUrl = "http://localhost:5173";
    private static final SecureRandom RNG = new SecureRandom();
    private static String generateUrlToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /**
     * Login
     * @param loginDTO
     * @return
     */
    @Override
    public String login(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new AuthException("Email is incorrect or does not exist"));
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            throw new AuthException("Incorrect email or password");
        }
        // if not verified, frontend will resend verify email
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new AuthException("Email not verified");
        }

        // Put userId into the token subject; put username and email into token claims
        return jwtUtils.generateJwt(
                user.getId().toString(),
                Map.of("username", user.getUsername(),
                        "email", user.getEmail(),
                        "version", user.getTokenVersion())
        );
    }

    /**
     * Register
     * only allow creating new users. If email exists, reject.
     * @param registerDTO
     */
    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        if (userRepository.existsByEmail(registerDTO.getEmail())) {
            throw new BusinessException("email exists");
        }
        if (userRepository.existsByUsername(registerDTO.getUsername())){
            throw new BusinessException("username exists");
        }
        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .avatar(DEFAULT_AVATAR)
                .tokenVersion(1)
                .emailVerified(false)
                .build();
        userRepository.save(user);
        sendVerifyEmail(user.getId(), user.getEmail());
    }

    // ---------------- Email Verification ---------------- //

    /**
     * Send verify email to user with verification token
     * 2 hours expiration
     * @param userId
     * @param email
     */
    @Override
    @Transactional
    public void sendVerifyEmail(Long userId, String email) {
        // delete old unused tokens
        emailTokenRepository.deleteByUserIdAndVerificationTokenIsNotNullAndUsedIsFalse(userId);
        // generate verify token and send email
        String token = generateUrlToken();
        emailTokenRepository.save(EmailToken.builder()
                .userId(userId)
                .verificationToken(token)
                .resetPasswordToken(null)
                .used(false)
                .expireTime(Instant.now().plusSeconds(2 * 3600))
                .build());

        // http://localhost:5173/verify-email?token=xxxx (Controller)
        String link = baseUrl + "/verify-email?token=" + token;
        log.info("Send verify email to user: {} with token: {}", email, token);

        // ensure email is sent only if transaction commits successfully
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                sendGridUtils.sendVerifyEmail(email, link);
            }
        });
    }

    /**
     * Resend verify email if user exists and is not verified
     * @param email
     */
    @Override
    @Transactional
    public void resendVerifyEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (Boolean.FALSE.equals(user.getEmailVerified())) {
                sendVerifyEmail(user.getId(), user.getEmail());
            }
        });
    }

    /**
     * Verify email by token, set emailVerified to true and return a jwt token
     * trigger: user click verify email link in email
     * @param token email verify token
     * @return jwt token
     */
    @Override
    @Transactional
    public String verifyEmailByToken(String token) {
        EmailToken emailToken = emailTokenRepository
                .findByVerificationTokenAndUsedIsFalseAndExpireTimeAfter(token, Instant.now())
                .orElseThrow(() -> new BusinessException("Invalid or expired token"));

        User user = userRepository.findById(emailToken.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("User {} email verified", user.getUsername());

        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);
        log.info("Token {} used", token);

        return jwtUtils.generateJwt(
                user.getId().toString(),
                Map.of("username", user.getUsername(),
                        "email", user.getEmail(),
                        "version", user.getTokenVersion())
        );
    }

    /**
     * Send reset password email to user with reset password token
     * 30 minutes expiration
     * @param email
     */
    @Override
    @Transactional
    public void sendForgotPasswordEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // delete old unused tokens
            emailTokenRepository.deleteByUserIdAndResetPasswordTokenIsNotNullAndUsedIsFalse(user.getId());
            String token = generateUrlToken();
            emailTokenRepository.save(EmailToken.builder()
                    .userId(user.getId())
                    .verificationToken(null)
                    .resetPasswordToken(token)
                    .used(false)
                    .expireTime(Instant.now().plusSeconds(30 * 60))
                    .build());
            // http://localhost:5173/reset-password?token=xxxx (Controller)
            String link = baseUrl + "/reset-password?token=" + token;
            log.info("Send reset password email to user: {} with token: {}", email, token);

            // ensure email is sent only if transaction commits successfully
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    sendGridUtils.sendResetEmail(email, link);
                }
            });
        });
    }

    /**
     * Reset password by token and set password to newPassword
     * trigger: user click reset password link in email
     * @param token
     * @param newPassword
     */
    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        EmailToken emailToken = emailTokenRepository
                .findByResetPasswordTokenAndUsedIsFalseAndExpireTimeAfter(token, Instant.now())
                .orElseThrow(() -> new BusinessException("Invalid or expired token"));

        User user = userRepository.findById(emailToken.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            emailTokenRepository.deleteByUserIdAndVerificationTokenIsNotNullAndUsedIsFalse(user.getId());
        }

        userRepository.save(user);
        log.info("User {} password reset", user.getUsername());

        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);
        log.info("resetPasswordToken {} used", token);
    }
}
