package com.demo.api.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordDTO {

    @Size(min=6,max=64)
    private String oldPassword; // Old password, 6-64 chars

    @Size(min=6,max=64)
    private String newPassword; // New password, 6-64 chars
}
