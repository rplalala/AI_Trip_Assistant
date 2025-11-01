package com.demo.api.task;

import cn.hutool.core.util.ObjectUtil;
import com.demo.api.model.EmailToken;
import com.demo.api.repository.*;
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
    private final TripRepository tripRepository;
    private final TripAttractionRepository tripAttractionRepository;
    private final TripHotelRepository tripHotelRepository;
    private final TripTransportationRepository tripTransportationRepository;
    private final TripDailySummaryRepository tripDailySummaryRepository;
    private final TripBookingQuoteRepository tripBookingQuoteRepository;
    private final TripInsightRepository insightRepository;
    private final TripWeatherRepository tripWeatherRepository;

    @Value("${aws.s3.dir-name}")
    private String dirName;

    /**
     * Clean redundant files in AWS S3 (exist in S3 but not in the database) every day at 2 AM
     * Temporarily only clean avatars.
     */
    @Scheduled(cron = "0 0 2 * * *")
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
     * Clean expired email tokens every 2 hours
     */
    @Scheduled(cron = "0 0 */2 * * *")
    @Transactional
    public void cleanup() {
        List<EmailToken> deleted = emailTokenRepository.deleteAllByExpireTimeBefore(Instant.now());
        log.info("cleaned {} tokens", deleted.size());
    }

    /**
     * Clean redundant data every 2 hours
     */
    @Scheduled(cron = "0 0 */2 * * *")
    @Transactional
    public void cleanRedundantData(){
        // find all redundant user ids
        List<Long> redundantUserIds = tripRepository.findRedundantUserIds();
        log.info("Number of redundant user ids: {}", redundantUserIds.size());
        if(ObjectUtil.isNotEmpty(redundantUserIds)){
            // find redundant user ids associated all trip ids
            List<Long> tripIds = tripRepository.findIdsByUserIdIn(redundantUserIds);
            log.info("Number of redundant trip ids: {}", tripIds.size());
            if (ObjectUtil.isNotEmpty(tripIds)) {
                tripWeatherRepository.deleteByTripIdIn(tripIds);
                insightRepository.deleteByTripIdIn(tripIds);
                tripBookingQuoteRepository.deleteByTripIdIn(tripIds);
                tripDailySummaryRepository.deleteByTripIdIn(tripIds);
                tripTransportationRepository.deleteByTripIdIn(tripIds);
                tripHotelRepository.deleteByTripIdIn(tripIds);
                tripAttractionRepository.deleteByTripIdIn(tripIds);
                tripRepository.deleteByUserIdIn(redundantUserIds);
            } else {
                log.info("No redundant trip ids");
            }

        } else {
            log.info("No redundant user ids");
        }
        log.info("Clean finished");
    }

}
