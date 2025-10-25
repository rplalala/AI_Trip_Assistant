package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.DeleteAccountDTO;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.UpdatePasswordDTO;
import com.demo.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 2. User Profile Page
 * Implement viewing and updating user profile.
 */
@Slf4j
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    /**
     * Get user profile details
     * @param userId The ID of the currently authenticated user
     * @return Detailed info of the currently authenticated user
     */
    @GetMapping
    public ApiRespond<ProfileDTO> getProfileDetail(@AuthenticationPrincipal String userId){
        // @AuthenticationPrincipal can obtain the principal info from the current authenticated user's token.
        // (In the Filter, the token principal is set to String userId)
        log.info("Current User: {}", userId);
        return ApiRespond.success(userService.getProfileDetail(Long.valueOf(userId)));
    }

    /**
     * Update user profile (avatar update at /upload/avatar)
     * @param userId The ID of the currently authenticated user
     * @param profileDTO The info to update
     * @return ——
     */
    @PutMapping
    public ApiRespond<Void> updateProfileDetail(@AuthenticationPrincipal String userId,
                                                @Valid @RequestBody ProfileDTO profileDTO){
        userService.updateProfileDetail(Long.valueOf(userId), profileDTO);
        return ApiRespond.success();
    }

    /**
     * Update user password
     * @param userId The ID of the currently authenticated user
     * @param passwordDTO New and old passwords
     * @return ——
     */
    @PutMapping("/pd")
    public ApiRespond<Void> updatePassword (@AuthenticationPrincipal String userId,
                                            @Valid @RequestBody UpdatePasswordDTO passwordDTO){
        if(passwordDTO.getOldPassword().equals(passwordDTO.getNewPassword())){
            return ApiRespond.error("New password cannot be the same as old password");
        }
        userService.updatePassword(Long.valueOf(userId), passwordDTO);
        return ApiRespond.success();
    }

    /**
     * Delete user account
     * @param userId
     * @param deleteAccountDTO
     * @return
     */
    @DeleteMapping
    public ApiRespond<Void> deleteUser(@AuthenticationPrincipal String userId,
                                       @RequestBody DeleteAccountDTO deleteAccountDTO){
        userService.deleteUser(Long.valueOf(userId), deleteAccountDTO);
        return ApiRespond.success();
    }

}
