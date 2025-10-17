package com.demo.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {

    @Size(min=3,max=30) // username不为null时，长度被限制在3-30
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "用户名只能包含字母、数字、下划线和短横线")
    private String username; // 用户名 3-30位，只能包含字母、数字、下划线和短横线。非必须

    @Min(0) // age不为null时，范围是0-150
    @Max(150)
    private Integer age; // 年龄 0-150。非必须

    @Min(1)
    @Max(2)
    private Integer gender; // 性别，非必须。 1: Male, 2: Female

    // 只读：请求体中的 email 会被忽略，响应中仍然返回
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String email; // 邮箱

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String avatar; // 用户头像。
}
