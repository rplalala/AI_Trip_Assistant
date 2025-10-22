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
 * 1. Login/Register
 * Implement user registration, login, and authentication.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LoginController {

    private final UserService userService;

    /**
     * Login
     * Verify account and password, return JWT token upon success.
     *
     * @param loginDTO Login information
     * @return JWT token
     */
    @PostMapping("/login")
    public ApiRespond<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ApiRespond.success(userService.login(loginDTO));
    }

    /**
     * Register
     * Create a new user.
     *
     * @param registerDTO Registration information
     * @return ——
     */
    @PostMapping("/register")
    public ApiRespond<Void> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return ApiRespond.success();
    }

}
