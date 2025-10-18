package com.demo.api.controller;

import com.demo.api.ApiRespond;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.UpdatePasswordDTO;
import com.demo.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 2. 用户详情页
 * 实现用户详情页的查看与更新功能
 */
@Slf4j
@RestController
@RequestMapping("/api/users/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    /**
     * 获取用户信息详情
     * @param userId 当前认证用户的id
     * @return 当前认证用户的详细信息
     */
    @GetMapping
    public ApiRespond<ProfileDTO> getProfileDetail(@AuthenticationPrincipal String userId){
        // @AuthenticationPrincipal 可以得到当前认证用户token中principal的信息。
        // （在Filter中，token的principal被设为String userId）
        log.info("Current User: {}", userId);
        return ApiRespond.success(userService.getProfileDetail(Long.valueOf(userId)));
    }

    /**
     * 更新用户详情页信息（头像更新在 /upload/avatar接口）
     * @param userId 当前认证用户的id
     * @param profileDTO 需要修改的信息
     * @return ——
     */
    @PutMapping
    public ApiRespond<Void> updateProfileDetail(@AuthenticationPrincipal String userId,
                                                @Valid @RequestBody ProfileDTO profileDTO){
        userService.updateProfileDetail(Long.valueOf(userId), profileDTO);
        return ApiRespond.success();
    }

    /**
     * 更新用户密码
     * @param userId 当前认证用户的id
     * @param passwordDTO 新、旧密码
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

}
