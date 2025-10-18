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
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "用户名只能包含字母、数字、下划线和短横线")
    private String username; // 注册用户名，3-30位，只能包含字母、数字、下划线和短横线。必填

    @NotBlank
    @Email
    private String email; // 注册邮箱。必填

    @NotBlank
    @Size(min=6,max=64)
    private String password; // 注册密码，6-64位，必填
}
