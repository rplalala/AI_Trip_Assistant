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
    private String email; // 用户登录邮箱。必填

    @NotBlank
    @Size(min=6,max=64)
    private String password; // 用户登录密码 6-64位。必填
}
