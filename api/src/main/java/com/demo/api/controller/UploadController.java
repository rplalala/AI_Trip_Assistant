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
 * 3. Upload images to AWS S3
 * Implement local image upload and external link upload. Supported formats: jpg, jpeg, png, gif, svg, webp
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final AwsS3Utils awsS3Utils;
    private final UserService userService;

    /**
     * Image upload. Local upload.
     * @param file Local image file
     * @return ASW S3 URL elec5620-stage2/
     */
    @PostMapping
    public ApiRespond<String> upload(@RequestParam("file") MultipartFile file) throws Exception{
        log.info("Upload Img File：{}", file.getOriginalFilename());
        if(file.isEmpty()){
            return ApiRespond.error("File dose not exist");
        }
        // 10 MB limit
        long maxSize = 10 * 1024 * 1024L;
        if (file.getSize() > maxSize) {
            return ApiRespond.error("File too large, max size is 10 MB");
        }

        return ApiRespond.success(awsS3Utils.upload(file.getInputStream(), file.getOriginalFilename(), false));
    }

    /**
     * Upload and update user avatar. Local upload.
     * @param file Avatar file
     * @return ASW S3 new avatar URL elec5620-stage2/avatars
     */
    @PostMapping("/avatar")
    public ApiRespond<String> uploadAvatar(@RequestParam("file") MultipartFile file
            , @AuthenticationPrincipal String userId) throws Exception{
        log.info("Upload avatar：{}", file.getOriginalFilename());
        if(file.isEmpty()){
            return ApiRespond.error("File dose not exist");
        }
        // 10 MB limit
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
     * Single file upload via external link
     * @param url External image URL
     * @return ASW S3 image URL
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
