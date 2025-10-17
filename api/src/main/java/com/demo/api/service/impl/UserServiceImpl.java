package com.demo.api.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.dto.UpdatePasswordDTO;
import com.demo.api.exception.AuthException;
import com.demo.api.exception.BusinessException;
import com.demo.api.model.User;
import com.demo.api.repository.UserRepository;
import com.demo.api.service.UserService;
import com.demo.api.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value( "${default.avatar-url}")
    private String DEFAULT_AVATAR; // 默认头像

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

        // 把userId放入token的subject，userName和email放到token的email
        String token = jwtUtils.generateJwt(
                user.getId().toString(),
                Map.of("username", user.getUsername(),
                        "email", user.getEmail(),
                        "version", user.getTokenVersion())
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
                .avatar(DEFAULT_AVATAR) // 默认头像
                .tokenVersion(1)
                .build();
        userRepository.save(user);
    }

    /**
     * 获取用户详情页信息
     * @param userId
     * @return
     */
    @Override
    @Transactional
    public ProfileDTO getProfileDetail(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        return new ProfileDTO(user.getUsername(), user.getAge(),
                user.getGender(), user.getEmail(), user.getAvatar());
    }

    /**
     * 更新用户详情页信息，可选 username, gender, age
     * @param userId
     * @param profileDTO
     */
    @Override
    @Transactional
    public void updateProfileDetail(Long userId, ProfileDTO profileDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        if (ObjectUtil.isNotEmpty(profileDTO.getUsername())
                && !profileDTO.getUsername().equals(user.getUsername())){
            if (userRepository.existsByUsernameAndIdNot(profileDTO.getUsername(), userId)){
                throw new BusinessException("new username exists");
            }
            user.setUsername(profileDTO.getUsername());
        }
        if (ObjectUtil.isNotEmpty(profileDTO.getGender())){
            user.setGender(profileDTO.getGender());
        }
        if (ObjectUtil.isNotEmpty(profileDTO.getAge())){
            user.setAge(profileDTO.getAge());
        }

        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // 并发兜底
            throw new BusinessException("username exists");
        }

    }

    /**
     * 更新用户密码
     * @param userId
     * @param passwordDTO
     */
    @Override
    @Transactional
    public void updatePassword(Long userId, UpdatePasswordDTO passwordDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())){
            throw new BusinessException("old password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(passwordDTO.getNewPassword()));
        // 更改后 jwt token版本 +1，之前分发的token失效
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    /**
     * 更新用户新上传的头像
     * @param userId
     * @param newAvatarUrl
     */
    @Override
    public void updateAvatar(Long userId, String newAvatarUrl) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        user.setAvatar(newAvatarUrl);
        userRepository.save(user);
    }

}
