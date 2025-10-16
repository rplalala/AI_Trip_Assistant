package com.demo.api.service.impl;

import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.exception.AuthException;
import com.demo.api.exception.BusinessException;
import com.demo.api.model.User;
import com.demo.api.repository.UserRepository;
import com.demo.api.service.UserService;
import com.demo.api.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 登录
     * @param loginDTO
     * @return
     */
    @Override
    public String login(LoginDTO loginDTO) {
        User user = userRepository.findByEmail(loginDTO.getEmail())
                .orElseThrow(() -> new AuthException("Email is incorrect or does not exist"));
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())){
            throw new AuthException("Incorrect email or password");
        }

        String token = jwtUtils.generateJwt(
                user.getId().toString(),
                Map.of("username", user.getUsername(), "email", user.getEmail())
        );

        return token;
    }

    /**
     * 注册
     * @param registerDTO
     */
    @Override
    public void register(RegisterDTO registerDTO) {
        if (userRepository.existsByUsername(registerDTO.getUsername())){
            throw new BusinessException("username exists");
        }
        if (userRepository.existsByEmail(registerDTO.getEmail())){
            throw new BusinessException("email exists");
        }

        User user = User.builder()
                .username(registerDTO.getUsername())
                .email(registerDTO.getEmail())
                .password(passwordEncoder.encode(registerDTO.getPassword()))
                .build();
        userRepository.save(user);
    }

}
