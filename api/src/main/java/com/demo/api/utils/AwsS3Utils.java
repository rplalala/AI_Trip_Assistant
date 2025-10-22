package com.demo.api.utils;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * AWS S3 utility class (@Value-injected configuration)
 * See AWS SDK for Java v2 official documentation
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_s3_code_examples.html
 */
@Slf4j
@Component
public class AwsS3Utils {

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.dir-name}")
    private String dirName;

    @Value("${aws.s3.cdn}")
    private String cdn;

    @Value("${aws.s3.accesskeyId}")
    private String accesskeyId;

    @Value("${aws.s3.secretAccessKey}")
    private String secretAccessKey;

    private S3Client newS3Client() {
        AwsBasicCredentials creds = AwsBasicCredentials.create(accesskeyId, secretAccessKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .build();
    }

    /**
     * 1. S3 single file upload
     *
     * @param in               file input stream
     * @param originalFilename original filename
     * @return Return URL (https://{cdn}/{dir/yyyy/MM/xxx.png})
     */
    public String upload(InputStream in, String originalFilename, boolean isAvatar) throws Exception {
        S3Client s3 = newS3Client();

        // If it is an avatar, put it under elec5620-stage2/avatars
        String baseDir = isAvatar ? (dirName + "/avatars") : dirName;

        // Directory and file name: rewrite the filename
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String objectKey = baseDir + "/" + dir + "/" + newFileName;

        // Read into byte[] to get content-length
        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(32 * 1024, in.available()));
        in.transferTo(bos);
        byte[] bytes = bos.toByteArray();

        String contentType = switch (suffix) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".svg" -> "image/svg+xml";
            case ".webp" -> "image/webp";
            default -> "application/octet-stream"; // Default type when file type is unknown
        };

        try (s3) {
            PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3.putObject(putReq, RequestBody.fromBytes(bytes));
        }

        return "https://" + cdn + "/" + objectKey;
    }

    /**
     * 1.1 Download image from external URL to memory, then upload, and return final URL (AWS S3)
     *
     * @param imageUrl external URL
     * @return Return URL (https://{cdn}/{dir/yyyy/MM/xxx.png})
     */
    public String uploadFromUrl(String imageUrl) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalArgumentException("Fetch image failed, http status=" + response.statusCode());
        }

        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (!contentType.isEmpty() && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Unsupported content-type: " + contentType);
        }

        long maxSize = 10 * 1024 * 1024L;
        long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
        if (contentLength > maxSize) {
            throw new IllegalArgumentException("File too large, max size is 10 MB");
        }

        // Infer file suffix
        String suffix = null;
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("jpeg") || ct.contains("jpg")) suffix = ".jpg";
        else if (ct.contains("png")) suffix = ".png";
        else if (ct.contains("gif")) suffix = ".gif";
        else if (ct.contains("svg")) suffix = ".svg";
        else if (ct.contains("webp")) suffix = ".webp";

        if (suffix == null) {
            String path = URI.create(imageUrl).getPath();
            int dot = path.lastIndexOf('.');
            if (dot != -1 && dot < path.length() - 1) {
                String ext = path.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.matches("jpg|jpeg|png|gif|svg|webp")) {
                    suffix = "." + (ext.equals("jpeg") ? "jpg" : ext);
                }
            }
        }
        if (suffix == null) suffix = ".jpg";

        // Read into memory and enforce 10 MB limit
        try (InputStream in = response.body(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > maxSize) {
                    throw new IllegalArgumentException("File too large, max size is 10 MB");
                }
                bos.write(buf, 0, n);
            }
            String s3Url = upload(new ByteArrayInputStream(bos.toByteArray()), "temp" + suffix, false);
            log.info("Upload from {} to {}", imageUrl, s3Url);
            return s3Url;
        }
    }

    /**
     * 2. List files in AWS S3 with the specified prefix (applicable when file count ≤ 1000)
     *
     * @param preFix  Prefix (e.g., elec5620-stage2/2025)
     * @param size    Maximum number to list (up to 1000)
     */
    public List<String> listFiles(String preFix, int size) throws Exception {
        S3Client s3 = newS3Client();
        if (size > 1000) {
            size = 1000;
            log.warn("A maximum of 1000 files is allowed, it has been automatically adjusted to 1000");
        }

        List<String> fileList = new ArrayList<>();
        try (s3) {
            ListObjectsV2Response resp = s3.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(preFix)
                    .maxKeys(size)
                    .build());
            List<S3Object> contents = resp.contents();
            if (ObjectUtil.isNotEmpty(contents)) {
                fileList = contents.stream().map(S3Object::key).toList();
            }
        }
        return fileList;
    }

    /**
     * 3. Paginated listing of files in AWS S3 with the specified prefix (applicable when file count > 1000)
     *
     * @param preFix   Prefix
     * @param pageSize Page size (max 1000)
     */
    public List<String> listPageAllFiles(String preFix, int pageSize) throws Exception {
        S3Client s3 = newS3Client();
        if (pageSize > 1000) {
            pageSize = 1000;
            log.warn("A maximum of 1000 files every time is allowed, it has been automatically adjusted to 1000");
        }

        List<String> fileList = new ArrayList<>();
        String continuationToken = null;

        try (s3) {
            ListObjectsV2Response resp;
            do {
                var builder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(preFix)
                        .maxKeys(pageSize);
                if (continuationToken != null) builder.continuationToken(continuationToken);

                resp = s3.listObjectsV2(builder.build());

                List<S3Object> contents = resp.contents();
                if (ObjectUtil.isNotEmpty(contents)) {
                    fileList.addAll(contents.stream().map(S3Object::key).toList());
                }
                continuationToken = resp.nextContinuationToken();
            } while (resp.isTruncated());
        }
        return fileList;
    }

    /**
     * 4. Delete a single file
     *
     * @param objectKey File key (without bucket name), e.g.: elec5620-stage2/2025/06/xxx.png
     */
    public void deleteFile(String objectKey) throws Exception {
        S3Client s3 = newS3Client();
        try (s3) {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());
        }
    }

    /**
     * 5. Batch delete multiple files (up to 1000 at a time; split into batches if exceeded)
     *
     * @param objectKeys List of keys to delete
     */
    public void batchDeleteFiles(List<String> objectKeys) throws Exception {
        S3Client s3 = newS3Client();
        try (s3) {
            for (int i = 0; i < objectKeys.size(); i += 1000) {
                int end = Math.min(i + 1000, objectKeys.size());
                List<String> subList = objectKeys.subList(i, end);
                log.info("Batch deleting files, current batch: {}, file count: {}", (i / 1000) + 1, subList.size());

                List<ObjectIdentifier> ids = new ArrayList<>(subList.size());
                for (String key : subList) {
                    ids.add(ObjectIdentifier.builder().key(key).build());
                }

                s3.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(ids).quiet(true).build())
                        .build());
            }
        }
    }
}
