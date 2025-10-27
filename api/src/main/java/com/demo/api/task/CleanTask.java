package com.demo.api.task;

import cn.hutool.core.util.ObjectUtil;
import com.demo.api.model.EmailToken;
import com.demo.api.repository.EmailTokenRepository;
import com.demo.api.repository.UserRepository;
import com.demo.api.utils.AwsS3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;

/**
 * Spring scheduled task: regularly clean up redundant files in AWS S3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CleanTask {
    private final AwsS3Utils awsS3Utils;
    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;

    @Value("${aws.s3.dir-name}")
    private String dirName;

    /**
     * Clean redundant files in AWS S3 (exist in S3 but not in the database) every 5 minutes.
     * Temporarily only clean avatars.
     * TODO: Clean all redundant image files (including user avatars and trip images).
     */
    @Scheduled(cron = "0 0 */1 * * *")
    public void fileClean() throws Exception {
        log.info("File clean task started...");
        // Get all user avatar URLs from the database
        List<String> dbFileUrls =  userRepository.findAllAvatar();
        List<String> dbFormatUrls = dbFileUrls.stream().map(url -> {
            try {
                return new URL(url).getPath().substring(1);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }).toList();
        log.info("Number of avatars in database: {}", dbFormatUrls.size());

        // Get all user avatar URLs in AWS S3
        List<String> awsS3AllFiles = awsS3Utils.listPageAllFiles(dirName + "/avatars/", 1000);
        log.info("Number of avatar images in AWS S3: {}", awsS3AllFiles.size());
        // If S3 is empty, do not perform delete operations.
        if(ObjectUtil.isNotEmpty(awsS3AllFiles) && ObjectUtil.isNotEmpty(dbFormatUrls)) {
            // Compare the two sets to find redundant images on AWS S3
            List<String> deleteFiles = awsS3AllFiles.stream()
                    .filter(awsS3File -> !dbFormatUrls.contains(awsS3File))
                    .toList();
            log.info("Number of files to delete: {}", deleteFiles.size());
            if(ObjectUtil.isNotEmpty(deleteFiles)){
                deleteFiles.forEach(s -> log.info("File to delete: {}", s));
                // Delete redundant images
                awsS3Utils.batchDeleteFiles(deleteFiles);
            }
        } else {
            log.info("No files to delete");
        }
        log.info("File clean task finished...");
    }

    /**
     * Clean expired email tokens every 5 minutes
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void cleanup() {
        List<EmailToken> deleted = emailTokenRepository.deleteAllByExpireTimeBefore(Instant.now());
        log.info("cleaned {} tokens", deleted.size());
    }
}
