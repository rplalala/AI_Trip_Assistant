package com.demo.api.utils;

import cn.hutool.core.util.ObjectUtil;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.*;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aliyun OSS utils
 * See Aliyun OSS Java SDK official documentation.
 * 支持：普通上传、URL抓取后上传、列举(单页 & 分页)、单文件删除、批量删除、头像特殊目录。
 */
@Slf4j
@Component
public class AliyunOSSUtils {
    private final OSS ossClient;
    public AliyunOSSUtils(@Value("${aliyun.oss.endpoint}") String ep,
                          @Value("${aliyun.oss.region}") String reg,
                          @Value("${aliyun.oss.accessKeyId:}") String ak,
                          @Value("${aliyun.oss.accessKeySecret:}") String sk){
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        DefaultCredentialProvider credentialsProvider =
                CredentialsProviderFactory.newDefaultCredentialProvider(ak, sk);
        this.ossClient =  OSSClientBuilder.create()
                .endpoint(ep)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(reg)
                .build();
    }

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.dirName}")
    private String dirName;

    @Value("${aliyun.oss.cdn}")
    private String cdnName;

    @PreDestroy
    public void close() {
        try {
            if (ossClient != null) ossClient.shutdown();
        } catch (Exception e) {
            log.debug("Ignore ossClient shutdown error", e);
        }
    }

    /**
     * 1. 单文件上传 (支持头像目录)。
     * 如果是头像：放置在 {dirName}/avatars 下；否则放在 {dirName}/yyyy/MM/uuid.xx
     *
     * @param in               文件输入流
     * @param originalFilename 原始文件名
     * @param isAvatar         是否头像
     * @return 访问 URL (https://{cdn}/{dir/yyyy/MM/xxx.png} 或 https://{cdn}/{dir/avatars/yyyy/MM/xxx.png})
     */
    public String upload(InputStream in, String originalFilename, boolean isAvatar) throws Exception {
        String baseDir = isAvatar ? (dirName + "/avatars") : dirName;
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String objectName = baseDir + "/" + dir + "/" + newFileName;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(32 * 1024, in.available()));
        in.transferTo(bos);
        byte[] bytes = bos.toByteArray();

        String contentType = switch (suffix.toLowerCase(Locale.ROOT)) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".svg" -> "image/svg+xml";
            case ".webp" -> "image/webp";
            default -> "application/octet-stream";
        };

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);
        metadata.setContentLength(bytes.length);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, bais, metadata);
            ossClient.putObject(putObjectRequest);
        }
        return "https://" + cdnName + "/" + objectName;
    }

    /**
     * 1.1 通过外部图片 URL 下载到内存后再上传到 OSS。
     * 限制：最大 10 MB；仅支持 image/*。
     *
     * @param imageUrl 外部图片地址
     * @param isAvatar 是否头像
     * @return 访问 URL
     */
    public String uploadFromUrl(String imageUrl, boolean isAvatar) throws Exception {
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

        try (InputStream body = response.body(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            long total = 0;
            int n;
            while ((n = body.read(buf)) != -1) {
                total += n;
                if (total > maxSize) {
                    throw new IllegalArgumentException("File too large, max size is 10 MB");
                }
                bos.write(buf, 0, n);
            }
            String url = upload(new ByteArrayInputStream(bos.toByteArray()), "temp" + suffix, isAvatar);
            log.info("Upload from {} to {}", imageUrl, url);
            return url;
        }
    }

    /**
     * 2. 列举指定前缀的文件 (文件数量 <= 1000)。
     * @param preFix 前缀 (例如: dirName/2025)
     * @param size   最多列举多少 (上限1000)
     */
    public List<String> listFiles(String preFix, int size) throws Exception {
        if (size > 1000) {
            size = 1000;
            log.warn("最多1000个文件，已自动调整为1000");
        }
        ObjectListing objectListing = ossClient.listObjects(new ListObjectsRequest(bucketName)
                .withMaxKeys(size)
                .withPrefix(preFix));
        List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
        if (ObjectUtil.isNotEmpty(sums)) {
            return sums.stream().map(OSSObjectSummary::getKey).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 3. 分页列举指定前缀的全部文件 (文件数量 > 1000)。
     * @param preFix 前缀
     * @param pageSize 每页大小 (上限1000)
     */
    public List<String> listPageAllFiles(String preFix, int pageSize) throws Exception {
        if (pageSize > 1000) {
            pageSize = 1000;
            log.warn("最多每页1000个文件，已自动调整为1000");
        }
        List<String> fileList = new ArrayList<>();
        String nextMarker = null;
        ObjectListing objectListing;
        do {
            objectListing = ossClient.listObjects(new ListObjectsRequest(bucketName)
                    .withPrefix(preFix)
                    .withMarker(nextMarker)
                    .withMaxKeys(pageSize));
            List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
            if (ObjectUtil.isNotEmpty(sums)) {
                for (OSSObjectSummary s : sums) fileList.add(s.getKey());
            }
            nextMarker = objectListing.getNextMarker();
        } while (objectListing.isTruncated());
        return fileList;
    }

    /**
     * 4. 删除单个文件。
     * @param objectName 完整路径 (不含 bucket 名)。
     */
    public void deleteFile(String objectName) throws Exception {
        ossClient.deleteObject(bucketName, objectName);
    }

    /**
     * 5. 批量删除多个文件 (每批最多1000)。
     * @param objectNames 文件完整路径列表。
     */
    public void batchDeleteFiles(List<String> objectNames) throws Exception {
        for (int i = 0; i < objectNames.size(); i += 1000) {
            int end = Math.min(i + 1000, objectNames.size());
            List<String> subList = objectNames.subList(i, end);
            log.info("批量删除文件，当前批次：{}，文件数：{}", (i / 1000) + 1, subList.size());
            DeleteObjectsRequest req = new DeleteObjectsRequest(bucketName)
                    .withKeys(subList)
                    .withEncodingType("url")
                    .withQuiet(true);
            ossClient.deleteObjects(req);
        }
    }
}
