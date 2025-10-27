package com.demo.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "email_tokens", indexes = {
        @Index(name = "idx_email_tokens_verification_token", columnList = "verification_token", unique = true),
        @Index(name = "idx_email_tokens_reset_password_token", columnList = "reset_password_token", unique = true),
        @Index(name = "idx_email_tokens_change_email_token", columnList = "change_email_token", unique = true),
        @Index(name = "idx_email_tokens_confirm_change_email_token", columnList = "confirm_change_email_token", unique = true)
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

    @Column(length = 128)
    private String changeEmailToken;

    // New: token sent to new email for confirmation
    @Column(length = 128)
    private String confirmChangeEmailToken;

    // New: the new email to be applied after confirmation
    @Column(length = 320)
    private String pendingNewEmail;

    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(nullable = false)
    private Instant expireTime;
}
