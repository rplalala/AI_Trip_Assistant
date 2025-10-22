package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 1. Login/Register
 * Implement user registration, login, and authentication.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Login
     * Verify account and password, return JWT token upon success.
     *
     * @param loginDTO Login information
     * @return JWT token
     */
    @PostMapping("/login")
    public ApiRespond<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        return ApiRespond.success(authService.login(loginDTO));
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
        authService.register(registerDTO);
        return ApiRespond.success();
    }

    /**
     * Verify email address
     * click email link http://localhost:5173/verify-email?token=xxxx (this GET) to verify email address
     * @param token
     * @return
     */
    @GetMapping("/verify-email")
    public ApiRespond<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmailByToken(token);
        return ApiRespond.success();
    }

    /**
     * Forgot password
     * input email first, send email to user with reset password link:
     * http://localhost:5173/reset-password?token=xxxx (Next Post)
     * @param email
     * @return
     */
    @PostMapping("/forgot-password")
    public ApiRespond<Void> forgotPassword(@RequestParam String email) {
        authService.sendForgotPasswordEmail(email);
        return ApiRespond.success();
    }

    /**
     * Reset password
     * token from email link + new password from user input
     * @param token
     * @param newPassword
     * @return
     */
    @PostMapping("/reset-password")
    public ApiRespond<Void> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        authService.resetPassword(token, newPassword);
        return ApiRespond.success();
    }

}
