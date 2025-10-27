package com.demo.api.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.demo.api.dto.DeleteAccountDTO;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.UpdatePasswordDTO;
import com.demo.api.exception.BusinessException;
import com.demo.api.model.EmailToken;
import com.demo.api.model.User;
import com.demo.api.repository.EmailTokenRepository;
import com.demo.api.repository.UserRepository;
import com.demo.api.service.UserService;
import com.demo.api.utils.SendGridUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final SendGridUtils sendGridUtils;
    private final EmailTokenRepository emailTokenRepository;
    private final String baseUrl = "http://localhost:5173";
    private static final SecureRandom RNG = new SecureRandom();
    private static String generateUrlToken() {
        byte[] buf = new byte[32];
        RNG.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /**
     * Get user profile details
     * @param userId
     * @return
     */
    @Override
    @Transactional
    public ProfileDTO getProfileDetail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        return new ProfileDTO(user.getUsername(), user.getAge(),
                user.getGender(), user.getEmail(), user.getAvatar());
    }

    /**
     * Update user profile details; optional fields: username, gender, age
     * @param userId
     * @param profileDTO
     */
    @Override
    @Transactional
    public void updateProfileDetail(Long userId, ProfileDTO profileDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        if (ObjectUtil.isNotEmpty(profileDTO.getUsername())
                && !profileDTO.getUsername().equals(user.getUsername())){
            if (userRepository.existsByUsernameAndIdNot(profileDTO.getUsername(), userId)){
                throw new BusinessException("new username exists");
            }
            user.setUsername(profileDTO.getUsername());
        }
        if (ObjectUtil.isNotEmpty(profileDTO.getGender())){
            user.setGender(profileDTO.getGender());
        }
        if (ObjectUtil.isNotEmpty(profileDTO.getAge())){
            user.setAge(profileDTO.getAge());
        }

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Concurrency fallback
            throw new BusinessException("username exists");
        }

    }

    /**
     * Update user password
     * @param userId
     * @param passwordDTO
     */
    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordDTO passwordDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())){
            throw new BusinessException("old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        // After the change, increment JWT token version by 1; previously issued tokens become invalid
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    /**
     * Update the newly uploaded user avatar
     * @param userId
     * @param newAvatarUrl
     */
    @Override
    @Transactional
    public void updateAvatar(Long userId, String newAvatarUrl) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        user.setAvatar(newAvatarUrl);
        userRepository.save(user);
    }

    /**
     * delete user by userId
     * @param userId
     */
    @Override
    @Transactional
    public void deleteUser(Long userId, DeleteAccountDTO deleteAccountDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        if (!passwordEncoder.matches(deleteAccountDTO.getVerifyPassword(), user.getPassword())){
            throw new BusinessException("password is incorrect");
        }
        userRepository.deleteById(userId);
    }


    /**
     * Send change email link to user
     * @param userId
     */
    @Override
    @Transactional
    public void sendChangeEmailLink(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        emailTokenRepository.deleteByUserIdAndChangeEmailTokenIsNotNullAndUsedIsFalse(user.getId());
        emailTokenRepository.deleteByUserIdAndConfirmChangeEmailTokenIsNotNullAndUsedIsFalse(user.getId());
        String token = generateUrlToken();
        emailTokenRepository.save(EmailToken.builder()
                .userId(user.getId())
                .verificationToken(null)
                .resetPasswordToken(null)
                .changeEmailToken(token)
                .confirmChangeEmailToken(null)
                .pendingNewEmail(null)
                .used(false)
                .expireTime(Instant.now().plusSeconds(30 * 60))
                .build());
        String link = baseUrl + "/change-email?token=" + token;
        log.info("Send change email link to user: {} with token: {}", user.getEmail(), token);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                sendGridUtils.sendChangeEmail(user.getEmail(), link);
            }
        });
    }

    /**
     * Change user email address
     * @param token
     * @param newEmail
     */
    @Override
    @Transactional
    public void changeEmail(String token, String newEmail) {
        EmailToken emailToken = verifyChangeEmailByToken(token);

        if (userRepository.existsByEmail(newEmail)) {
            throw new BusinessException("email exists");
        }

        // generate confirmation token and send email to new address
        String confirmToken = generateUrlToken();
        emailToken.setConfirmChangeEmailToken(confirmToken);
        emailToken.setPendingNewEmail(newEmail);
        emailToken.setExpireTime(Instant.now().plusSeconds(30 * 60));
        emailTokenRepository.save(emailToken);

        String link = baseUrl + "/confirm-change-email?token=" + confirmToken;
        log.info("Send confirm change email to new address: {} with token: {}", newEmail, confirmToken);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                sendGridUtils.sendChangeEmail(newEmail, link);
            }
        });
    }

    @Override
    public EmailToken verifyChangeEmailByToken(String token) {
        return emailTokenRepository
                .findByChangeEmailTokenAndUsedIsFalseAndExpireTimeAfter(token, Instant.now())
                .orElseThrow(() -> new BusinessException("Invalid or expired token"));

    }

    /**
     * Confirm change email address
     * @param token
     */
    @Override
    @Transactional
    public void confirmChangeEmail(String token) {
        EmailToken emailToken = emailTokenRepository
                .findByConfirmChangeEmailTokenAndUsedIsFalseAndExpireTimeAfter(token, Instant.now())
                .orElseThrow(() -> new BusinessException("Invalid or expired token"));

        String newEmail = emailToken.getPendingNewEmail();
        if (newEmail == null || newEmail.isBlank()) {
            throw new BusinessException("No pending email to apply");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new BusinessException("email exists");
        }

        User user = userRepository.findById(emailToken.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        userRepository.save(user);
        log.info("User {} email changed from {} to {} after confirmation", user.getUsername(), oldEmail, newEmail);

        emailToken.setUsed(true);
        emailTokenRepository.save(emailToken);
    }


}
