package com.demo.api.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.demo.api.dto.DeleteAccountDTO;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.UpdatePasswordDTO;
import com.demo.api.exception.BusinessException;
import com.demo.api.model.User;
import com.demo.api.repository.UserRepository;
import com.demo.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get user profile details
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
     * Update user profile details; optional fields: username, gender, age
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
            // Concurrency fallback
            throw new BusinessException("username exists");
        }

    }

    /**
     * Update user password
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
        // After the change, increment JWT token version by 1; previously issued tokens become invalid
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
    }

    /**
     * Update the newly uploaded user avatar
     * @param userId
     * @param newAvatarUrl
     */
    @Override
    @Transactional
    public void updateAvatar(Long userId, String newAvatarUrl) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        user.setAvatar(newAvatarUrl);
        userRepository.save(user);
    }

    /**
     * delete user by userId
     * @param userId
     */
    @Override
    public void deleteUser(Long userId, DeleteAccountDTO deleteAccountDTO) {
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException("user not found"));
        if (!passwordEncoder.matches(deleteAccountDTO.getVerifyPassword(), user.getPassword())){
            throw new BusinessException("password is incorrect");
        }
        userRepository.deleteById(userId);
    }


}
