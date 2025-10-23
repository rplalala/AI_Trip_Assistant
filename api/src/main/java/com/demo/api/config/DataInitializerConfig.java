package com.demo.api.config;

import com.demo.api.model.User;
import com.demo.api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializerConfig {

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = User.builder()
                        .username("admin")
                        .email("admin@admin.com")
                        .password(passwordEncoder.encode("admin@admin.com"))
                        .emailVerified(true)
                        .tokenVersion(1)
                        .build();
                userRepository.save(admin);
                System.out.println("Admin user created: email=admin@admin.com, password=admin@admin.com");
            }
        };
    }
}
