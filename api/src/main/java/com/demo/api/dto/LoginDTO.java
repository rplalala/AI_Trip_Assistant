package com.demo.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {

    @NotBlank
    @Email
    private String email; // User login email. Required

    @NotBlank
    @Size(min=6,max=64)
    private String password; // User login password, 6-64 chars. Required
}
