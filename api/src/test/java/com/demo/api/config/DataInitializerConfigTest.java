package com.demo.api.config;

import com.demo.api.model.User;
import com.demo.api.repository.UserRepository;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DataInitializerConfigTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private final DataInitializerConfig config = new DataInitializerConfig();

    @Test
    void initDatabase_whenAdminMissing_createsDefaultAccount() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("admin@admin.com")).thenReturn("encoded");

        CommandLineRunner runner = config.initDatabase(userRepository, passwordEncoder);
        runner.run();

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getEmail()).isEqualTo("admin@admin.com");
        assertThat(saved.getEmailVerified()).isTrue();
        assertThat(saved.getPassword()).isEqualTo("encoded");
    }

    @Test
    void initDatabase_whenAdminAlreadyExists_doesNothing() throws Exception {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        CommandLineRunner runner = config.initDatabase(userRepository, passwordEncoder);
        runner.run();

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }
}
