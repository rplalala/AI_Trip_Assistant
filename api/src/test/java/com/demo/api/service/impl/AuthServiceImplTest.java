package com.demo.api.service.impl;

import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.exception.AuthException;
import com.demo.api.exception.BusinessException;
import com.demo.api.model.EmailToken;
import com.demo.api.model.User;
import com.demo.api.repository.EmailTokenRepository;
import com.demo.api.repository.UserRepository;
import com.demo.api.support.TransactionTestUtils;
import com.demo.api.utils.JwtUtils;
import com.demo.api.utils.SendGridUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailTokenRepository emailTokenRepository;
    @Mock
    private SendGridUtils sendGridUtils;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void tearDown() {
        TransactionTestUtils.clear();
    }

    @Test
    void login_withValidCredentials_returnsJwt() {
        LoginDTO dto = new LoginDTO();
        dto.setEmail("demo@example.com");
        dto.setPassword("secret");

        User user = User.builder()
                .id(99L)
                .email("demo@example.com")
                .password("encoded")
                .username("demo")
                .emailVerified(true)
                .tokenVersion(3)
                .build();

        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(true);
        when(jwtUtils.generateJwt(eq("99"), any(Map.class))).thenReturn("jwt-token");

        String token = authService.login(dto);

        assertThat(token).isEqualTo("jwt-token");
        verify(jwtUtils).generateJwt(eq("99"), any(Map.class));
    }

    @Test
    void login_whenPasswordIncorrect_throwsAuthException() {
        LoginDTO dto = new LoginDTO();
        dto.setEmail("demo@example.com");
        dto.setPassword("wrong");

        User user = User.builder()
                .email("demo@example.com")
                .password("encoded")
                .emailVerified(true)
                .build();

        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Incorrect email or password");
    }

    @Test
    void register_persistsUserAndDispatchesVerificationEmail() {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("new@example.com");
        dto.setPassword("secret");
        dto.setUsername("new-user");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(55L);
            return saved;
        });

        TransactionTestUtils.begin();
        authService.register(dto);

        verify(userRepository).save(any(User.class));
        verify(emailTokenRepository).deleteByUserIdAndVerificationTokenIsNotNullAndUsedIsFalse(55L);
        verify(emailTokenRepository).save(any(EmailToken.class));

        TransactionTestUtils.runAfterCommitCallbacks();
        verify(sendGridUtils).sendVerifyEmail(eq("new@example.com"), contains("verify-email?token="));
    }

    @Test
    void register_whenEmailExists_throwsBusinessException() {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("dup@example.com");

        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("email exists");
    }

    @Test
    void verifyEmailByToken_marksTokenUsedAndReturnsJwt() {
        EmailToken token = EmailToken.builder()
                .id(10L)
                .userId(77L)
                .used(false)
                .verificationToken("token-123")
                .expireTime(Instant.now().plusSeconds(60))
                .build();
        User user = User.builder()
                .id(77L)
                .username("demo")
                .email("demo@example.com")
                .tokenVersion(2)
                .build();

        when(emailTokenRepository.findByVerificationTokenAndUsedIsFalseAndExpireTimeAfter(eq("token-123"), any()))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(77L)).thenReturn(Optional.of(user));
        when(jwtUtils.generateJwt(eq("77"), any(Map.class))).thenReturn("jwt");

        String jwt = authService.verifyEmailByToken("token-123");

        assertThat(jwt).isEqualTo("jwt");
        assertThat(user.getEmailVerified()).isTrue();
        verify(emailTokenRepository).save(token);
    }

    @Test
    void resetPassword_updatesPasswordAndClearsVerificationTokens() {
        EmailToken token = EmailToken.builder()
                .userId(88L)
                .resetPasswordToken("reset-1")
                .used(false)
                .expireTime(Instant.now().plusSeconds(60))
                .build();
        User user = User.builder()
                .id(88L)
                .password("old-hash")
                .tokenVersion(5)
                .emailVerified(false)
                .build();

        when(emailTokenRepository.findByResetPasswordTokenAndUsedIsFalseAndExpireTimeAfter(eq("reset-1"), any()))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(88L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-secret")).thenReturn("new-hash");

        authService.resetPassword("reset-1", "new-secret");

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(user.getTokenVersion()).isEqualTo(6);
        assertThat(user.getEmailVerified()).isTrue();
        verify(emailTokenRepository).deleteByUserIdAndVerificationTokenIsNotNullAndUsedIsFalse(88L);
        verify(emailTokenRepository).save(token);
    }

    @Test
    void resendVerifyEmail_onlyForUnverifiedUsers() {
        User user = User.builder()
                .id(44L)
                .email("resend@example.com")
                .emailVerified(false)
                .build();
        when(userRepository.findByEmail("resend@example.com")).thenReturn(Optional.of(user));

        TransactionTestUtils.begin();
        authService.resendVerifyEmail("resend@example.com");
        TransactionTestUtils.runAfterCommitCallbacks();

        verify(emailTokenRepository).deleteByUserIdAndVerificationTokenIsNotNullAndUsedIsFalse(44L);
        verify(sendGridUtils).sendVerifyEmail(eq("resend@example.com"), contains("verify-email?token="));
    }

    @Test
    void sendForgotPasswordEmail_createsTokenAndSendsResetLink() {
        User user = User.builder()
                .id(66L)
                .email("forgot@example.com")
                .build();
        when(userRepository.findByEmail("forgot@example.com")).thenReturn(Optional.of(user));

        TransactionTestUtils.begin();
        authService.sendForgotPasswordEmail("forgot@example.com");

        verify(emailTokenRepository).deleteByUserIdAndResetPasswordTokenIsNotNullAndUsedIsFalse(66L);
        verify(emailTokenRepository).save(any(EmailToken.class));

        TransactionTestUtils.runAfterCommitCallbacks();
        verify(sendGridUtils).sendResetEmail(eq("forgot@example.com"), contains("reset-password?token="));
    }
}
