package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 1. 登录/注册
 * 实现用户注册，登录，认证功能
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;

    /**
     * 登录
     * 验证账号与密码，登录成功后返回 JWT令牌
     *
     * @param loginDTO 登录信息
     * @return JWT令牌
     */
    @PostMapping("/login")
    public ApiRespond<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ApiRespond.success(userService.login(loginDTO));
    }

    /**
     * 注册
     * 创建新用户
     *
     * @param registerDTO 注册信息
     * @return ——
     */
    @PostMapping("/register")
    public ApiRespond<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return ApiRespond.success();
    }

}
