package com.demo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordDTO {

    @NotBlank
    @Size(min=6,max=64)
    private String oldPassword; // 旧密码 6-64位

    @NotBlank
    @Size(min=6,max=64)
    private String newPassword; // 新密码 6-64位
}
