package com.demo.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO {

    @NotBlank
    @Size(min=3,max=30)
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String username; // Registration username, 3-30 chars, only letters, numbers, underscores, and hyphens. Required

    @NotBlank
    @Email
    private String email; // Registration email. Required

    @NotBlank
    @Size(min=6,max=64)
    private String password; // Registration password, 6-64 chars. Required
}
