package com.demo.api.service;

import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.model.EmailToken;

public interface AuthService {
    String login(LoginDTO loginDTO);
    void register(RegisterDTO registerDTO);
    void sendVerifyEmail(Long userId, String email);
    String verifyEmailByToken(String token);
    void sendForgotPasswordEmail(String email);
    void resetPassword(String token, String newPassword);
    void resendVerifyEmail(String email);
    EmailToken verifyResetPasswordEmailByToken(String token);
}
