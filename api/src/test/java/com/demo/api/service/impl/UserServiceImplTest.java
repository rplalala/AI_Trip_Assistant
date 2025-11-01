package com.demo.api.service.impl;

import com.demo.api.dto.DeleteAccountDTO;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.UpdatePasswordDTO;
import com.demo.api.exception.BusinessException;
import com.demo.api.model.EmailToken;
import com.demo.api.model.User;
import com.demo.api.repository.*;
import com.demo.api.support.TransactionTestUtils;
import com.demo.api.utils.SendGridUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TripRepository tripRepository;
    @Mock private TripAttractionRepository tripAttractionRepository;
    @Mock private TripHotelRepository tripHotelRepository;
    @Mock private TripTransportationRepository tripTransportationRepository;
    @Mock private TripDailySummaryRepository tripDailySummaryRepository;
    @Mock private TripBookingQuoteRepository tripBookingQuoteRepository;
    @Mock private TripInsightRepository tripInsightRepository;
    @Mock private TripWeatherRepository tripWeatherRepository;
    @Mock private SendGridUtils sendGridUtils;
    @Mock private EmailTokenRepository emailTokenRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @AfterEach
    void cleanupTransactions() {
        TransactionTestUtils.clear();
    }

    @Test
    void getProfileDetail_returnsUserProfile() {
        User user = User.builder()
                .id(5L)
                .username("demo")
                .age(30)
                .gender(2)
                .email("demo@example.com")
                .avatar("https://cdn/avatar.png")
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        ProfileDTO dto = userService.getProfileDetail(5L);

        assertThat(dto.getUsername()).isEqualTo("demo");
        assertThat(dto.getEmail()).isEqualTo("demo@example.com");
    }

    @Test
    void updateProfileDetail_overwritesChanges() {
        User user = User.builder()
                .id(9L)
                .username("old")
                .gender(1)
                .age(25)
                .build();
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("new-name", 9L)).thenReturn(false);

        ProfileDTO update = new ProfileDTO("new-name", 26, 2, "user@example.com", "avatar.png");
        userService.updateProfileDetail(9L, update);

        assertThat(user.getUsername()).isEqualTo("new-name");
        assertThat(user.getGender()).isEqualTo(2);
        assertThat(user.getAge()).isEqualTo(26);
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileDetail_whenUsernameExists_throwsBusinessException() {
        User user = User.builder()
                .id(1L)
                .username("current")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameAndIdNot("taken", 1L)).thenReturn(true);

        ProfileDTO update = new ProfileDTO("taken", null, null, null, null);
        assertThatThrownBy(() -> userService.updateProfileDetail(1L, update))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("username exists");
    }

    @Test
    void updatePassword_withValidOldPassword_updatesHashAndVersion() {
        User user = User.builder()
                .id(11L)
                .password("hash-old")
                .tokenVersion(2)
                .build();
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "hash-old")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("hash-new");

        userService.updatePassword(11L, new UpdatePasswordDTO("old", "new"));

        assertThat(user.getPassword()).isEqualTo("hash-new");
        assertThat(user.getTokenVersion()).isEqualTo(3);
        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_removesTripsAndUser() {
        User user = User.builder()
                .id(77L)
                .password("hash")
                .build();
        when(userRepository.findById(77L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(tripRepository.findIdsByUserIdIn(List.of(77L))).thenReturn(List.of(101L, 102L));

        DeleteAccountDTO dto = new DeleteAccountDTO();
        dto.setVerifyPassword("secret");
        userService.deleteUser(77L, dto);

        verify(tripWeatherRepository).deleteByTripIdIn(List.of(101L, 102L));
        verify(tripRepository).deleteByUserIdIn(List.of(77L));
        verify(userRepository).deleteById(77L);
    }

    @Test
    void sendChangeEmailLink_generatesTokenAndEmailsUser() {
        User user = User.builder()
                .id(12L)
                .email("current@example.com")
                .build();
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));

        TransactionTestUtils.begin();
        userService.sendChangeEmailLink(12L);

        verify(emailTokenRepository).deleteByUserIdAndChangeEmailTokenIsNotNullAndUsedIsFalse(12L);
        verify(emailTokenRepository).deleteByUserIdAndConfirmChangeEmailTokenIsNotNullAndUsedIsFalse(12L);
        verify(emailTokenRepository).save(any(EmailToken.class));

        TransactionTestUtils.runAfterCommitCallbacks();
        verify(sendGridUtils).sendChangeEmail(eq("current@example.com"), contains("change-email?token="));
    }

    @Test
    void changeEmail_assignsConfirmTokenAndQueuesEmail() {
        EmailToken token = EmailToken.builder()
                .userId(42L)
                .changeEmailToken("change-token")
                .used(false)
                .expireTime(Instant.now().plusSeconds(60))
                .build();
        when(emailTokenRepository.findByChangeEmailTokenAndUsedIsFalseAndExpireTimeAfter(eq("change-token"), any()))
                .thenReturn(Optional.of(token));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

        TransactionTestUtils.begin();
        userService.changeEmail("change-token", "new@example.com");

        ArgumentCaptor<EmailToken> captor = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getPendingNewEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getConfirmChangeEmailToken()).isNotBlank();

        TransactionTestUtils.runAfterCommitCallbacks();
        verify(sendGridUtils).sendChangeEmail(eq("new@example.com"), contains("confirm-change-email?token="));
    }

    @Test
    void confirmChangeEmail_appliesPendingEmail() {
        EmailToken token = EmailToken.builder()
                .userId(33L)
                .confirmChangeEmailToken("confirm-token")
                .pendingNewEmail("applied@example.com")
                .used(false)
                .expireTime(Instant.now().plusSeconds(60))
                .build();
        User user = User.builder()
                .id(33L)
                .username("demo")
                .email("old@example.com")
                .build();

        when(emailTokenRepository.findByConfirmChangeEmailTokenAndUsedIsFalseAndExpireTimeAfter(eq("confirm-token"), any()))
                .thenReturn(Optional.of(token));
        when(userRepository.findById(33L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("applied@example.com")).thenReturn(false);

        userService.confirmChangeEmail("confirm-token");

        assertThat(user.getEmail()).isEqualTo("applied@example.com");
        assertThat(token.isUsed()).isTrue();
        verify(emailTokenRepository).save(token);
    }
}
