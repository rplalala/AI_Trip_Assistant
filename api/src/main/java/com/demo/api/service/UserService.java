package com.demo.api.service;

import com.demo.api.dto.DeleteAccountDTO;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.UpdatePasswordDTO;

public interface UserService {

    ProfileDTO getProfileDetail(Long userId);

    void updateProfileDetail(Long userId, ProfileDTO profileDTO);

    void updatePassword(Long userId, UpdatePasswordDTO passwordDTO);

    void updateAvatar(Long userId, String newAvatarUrl);

    void deleteUser(Long userId, DeleteAccountDTO deleteAccountDTO);

    void sendChangeEmailLink(Long userId);
    void changeEmail(String token, String newEmail);
    void confirmChangeEmail(String token);
}
