package com.demo.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", schema = "public", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_user_email", columnNames = "email")
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

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String email;

    @Column
    private Integer age;

    @Column
    private Integer gender; // Gender, 1: Male, 2: Female

    @Column(columnDefinition = "TEXT")
    private String avatar;

    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 1; // Default 1

}
