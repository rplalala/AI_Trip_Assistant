package com.demo.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "email_tokens", indexes = {
        @Index(name = "idx_email_tokens_verification_token", columnList = "verification_token", unique = true),
        @Index(name = "idx_email_tokens_reset_password_token", columnList = "reset_password_token", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailToken extends BaseModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 128)
    private String verificationToken;

    @Column(length = 128)
    private String resetPasswordToken;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(nullable = false)
    private Instant expireTime;
}
