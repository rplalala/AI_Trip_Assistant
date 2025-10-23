package com.demo.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountDTO {
    @NotBlank(message = "Password is required")
    private String verifyPassword;
}
