package com.demo.api.service;

import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.dto.UpdatePasswordDTO;

public interface UserService {
    String login(LoginDTO loginDTO);

    void register(RegisterDTO registerDTO);

    ProfileDTO getProfileDetail(Long userId);

    void updateProfileDetail(Long userId, ProfileDTO profileDTO);

    void updatePassword(Long userId, UpdatePasswordDTO passwordDTO);

    void updateAvatar(Long userId, String newAvatarUrl);
}
