package com.demo.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_user_email", columnNames = "email")
        },
        indexes = {
                @Index(name = "idx_user_email", columnList = "email")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String avatar;

}
