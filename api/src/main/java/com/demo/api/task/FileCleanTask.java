package com.demo.api.task;

import cn.hutool.core.util.ObjectUtil;
import com.demo.api.repository.UserRepository;
import com.demo.api.utils.AwsS3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Spring定时任务：定时清理Aws s3内冗余的文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileCleanTask {
    private final AwsS3Utils awsS3Utils;
    private final UserRepository userRepository;

    @Value("${aws.s3.dir-name}")
    private String dirName;

    /**
     * 每天凌晨2点定时清理Aws s3内冗余的文件（s3上有，数据库里没有）
     * 暂时只实现清理头像
     * TODO: 清理所有冗余的图像文件。（包括用户头像、行程图片）
     */
    // @Scheduled(cron = "10/10 * * * * *") // test every 10 seconds
    @Scheduled(cron = "0 0 2 * * *")
    public void clean() throws Exception {
        log.warn("File clean task started...");
        // 获取数据库中所有用户头像的url
        List<String> dbFileUrls =  userRepository.findAllAvatar();
        List<String> dbFormatUrls = dbFileUrls.stream().map(url -> {
            try {
                return new URL(url).getPath().substring(1);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toList();
        log.info("Number of avatars in database: {}", dbFormatUrls.size());

        // 获取AWS S3 上所有用户头像的url
        List<String> awsS3AllFiles = awsS3Utils.listPageAllFiles(dirName + "/avatars/", 1000);
        log.info("Number of avatar images in AWS S3: {}", awsS3AllFiles.size());
        // s3为空时，不执行删除操作。
        if(ObjectUtil.isNotEmpty(awsS3AllFiles) && ObjectUtil.isNotEmpty(dbFormatUrls)) {
            // 比较两个集合，找出aws s3上多余的图片集合
            List<String> deleteFiles = awsS3AllFiles.stream()
                    .filter(awsS3File -> !dbFormatUrls.contains(awsS3File))
                    .toList();
            log.info("Number of files to delete: {}", deleteFiles.size());
            if(ObjectUtil.isNotEmpty(deleteFiles)){
                deleteFiles.forEach(s -> log.info("File to delete: {}", s));
                // 删除多余的图片
                awsS3Utils.batchDeleteFiles(deleteFiles);
            }
        } else {
            log.info("No files to delete");
        }
        log.warn("File clean task finished...");
    }
}
