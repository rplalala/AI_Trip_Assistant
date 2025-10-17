package com.demo.api.controller;

import cn.hutool.core.util.ObjectUtil;
import com.demo.api.ApiRespond;
import com.demo.api.service.UserService;
import com.demo.api.utils.AwsS3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 3. 上传图片到AWS S3
 * 实现本地图片上传、外链图片上传。支持格式 jpg,jpeg,png,gif,svg,webp
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final AwsS3Utils awsS3Utils;
    private final UserService userService;

    /**
     * 图片上传。 本地上传。
     * @param file 本地图片文件
     * @return ASW S3 地址 elec5620-stage2/
     */
    @PostMapping
    public ApiRespond<String> upload(@RequestParam("file") MultipartFile file) throws Exception{
        log.info("Upload Img File：{}", file.getOriginalFilename());
        if(file.isEmpty()){
            return ApiRespond.error("File dose not exist");
        }
        // 10 MB 上限
        long maxSize = 10 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            return ApiRespond.error("File too large, max size is 10 MB");
        }

        return ApiRespond.success(awsS3Utils.upload(file.getInputStream(), file.getOriginalFilename(), false));
    }

    /**
     * 用户头像上传并更新。 本地上传
     * @param file 头像文件
     * @return ASW S3 新头像地址 elec5620-stage2/avatars
     */
    @PostMapping("/avatar")
    public ApiRespond<String> uploadAvatar(@RequestParam("file") MultipartFile file
            , @AuthenticationPrincipal String userId) throws Exception{
        log.info("Upload avatar：{}", file.getOriginalFilename());
        if(file.isEmpty()){
            return ApiRespond.error("File dose not exist");
        }
        // 10 MB 上限
        long maxSize = 10 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            return ApiRespond.error("File too large, max size is 10 MB");
        }

        String newAvatarUrl = awsS3Utils.upload(file.getInputStream(), file.getOriginalFilename(), true);
        log.info("Upload avatar success, new avatar url: {}", newAvatarUrl);
        userService.updateAvatar(Long.valueOf(userId), newAvatarUrl);
        return ApiRespond.success(newAvatarUrl);
    }

    /**
     * 单文件上传 外链上传
     * @param url 外部图片链接
     * @return ASW S3 图片地址
     */
    @PostMapping("/link")
    public ApiRespond<String> uploadByUrl(@RequestParam("url") String url) throws Exception {
        if (ObjectUtil.isEmpty(url)) {
            return ApiRespond.error("URL cannot be empty");
        }
        log.info("Upload Img Url：{}", url);
        return ApiRespond.success(awsS3Utils.uploadFromUrl(url));
    }
}
