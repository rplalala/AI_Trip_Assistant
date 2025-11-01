package com.demo.api.utils;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsS3UtilsTest {

    @Mock
    private S3Client s3Client;

    private AwsS3Utils utils;
    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        utils = new AwsS3Utils("access", "secret", "ap-southeast-2");
        ReflectionTestUtils.setField(utils, "s3Client", s3Client);
        ReflectionTestUtils.setField(utils, "bucketName", "bucket");
        ReflectionTestUtils.setField(utils, "dirName", "root");
        ReflectionTestUtils.setField(utils, "cdn", "cdn.example.com");
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @DisplayName("upload stores object using derived key and returns CDN url")
    @Test
    void upload_putsObject() throws Exception {
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        String url = utils.upload(new ByteArrayInputStream("hello".getBytes()), "photo.png", false);

        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("bucket");
        assertThat(request.key()).startsWith("root/");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(url).startsWith("https://cdn.example.com/root/");
    }

    @DisplayName("uploadFromUrl downloads content and delegates to upload")
    @Test
    void uploadFromUrl_fetchesRemoteResource() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("Content-Type", "image/jpeg")
                .setBody(new Buffer().write(new byte[]{1, 2, 3}))
        );

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        when(s3Client.putObject(requestCaptor.capture(), any(RequestBody.class))).thenReturn(null);

        String url = utils.uploadFromUrl(server.url("/avatar.jpg").toString(), true);

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertThat(url).contains("root/avatars/");
    }

    @DisplayName("upload supports avatar directory and default content type")
    @Test
    void upload_whenAvatarUsesAvatarDirectory() throws Exception {
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);

        String url = utils.upload(new ByteArrayInputStream("avatar".getBytes()), "profile.bin", true);

        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());
        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.key()).contains("root/avatars/");
        assertThat(request.contentType()).isEqualTo("application/octet-stream");
        assertThat(url).contains("root/avatars/");
    }

    @DisplayName("uploadFromUrl throws when HTTP status not successful")
    @Test
    void uploadFromUrl_whenStatusNotOk_throws() {
        server.enqueue(new MockResponse().setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST));

        assertThatThrownBy(() -> utils.uploadFromUrl(server.url("/bad").toString(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status=400");
    }

    @DisplayName("uploadFromUrl infers suffix when content type missing")
    @Test
    void uploadFromUrl_whenContentTypeMissing_usesPathExtension() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(new Buffer().write(new byte[]{1, 2, 3, 4})));

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        when(s3Client.putObject(requestCaptor.capture(), any(RequestBody.class))).thenReturn(null);

        String url = utils.uploadFromUrl(server.url("/image.JPEG").toString(), false);

        assertThat(url).endsWith(".jpg");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @DisplayName("uploadFromUrl enforces 10MB limit")
    @Test
    void uploadFromUrl_whenFileTooLarge_throws() {
        byte[] large = new byte[10 * 1024 * 1024 + 5];
        server.enqueue(new MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("Content-Type", "image/png")
                .setBody(new Buffer().write(large)));

        assertThatThrownBy(() -> utils.uploadFromUrl(server.url("/big.png").toString(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File too large");
    }

    @DisplayName("listFiles returns keys from single response")
    @Test
    void listFiles_returnsKeys() throws Exception {
        ListObjectsV2Response response = ListObjectsV2Response.builder()
                .contents(List.of(
                        S3Object.builder().key("root/a.png").build(),
                        S3Object.builder().key("root/b.png").build()
                ))
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(response);

        List<String> keys = utils.listFiles("root", 100);

        assertThat(keys).containsExactly("root/a.png", "root/b.png");
    }

    @DisplayName("listFiles caps size at 1000")
    @Test
    void listFiles_whenSizeTooLarge_capsToLimit() throws Exception {
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(ListObjectsV2Response.builder().contents(List.of()).build());

        utils.listFiles("root", 1500);

        ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client).listObjectsV2(captor.capture());
        assertThat(captor.getValue().maxKeys()).isEqualTo(1000);
    }

    @DisplayName("listPageAllFiles iterates through truncated responses")
    @Test
    void listPageAllFiles_handlesPagination() throws Exception {
        ListObjectsV2Response first = ListObjectsV2Response.builder()
                .isTruncated(true)
                .nextContinuationToken("token")
                .contents(List.of(S3Object.builder().key("root/1.png").build()))
                .build();
        ListObjectsV2Response second = ListObjectsV2Response.builder()
                .isTruncated(false)
                .contents(List.of(S3Object.builder().key("root/2.png").build()))
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                .thenReturn(first)
                .thenReturn(second);

        List<String> keys = utils.listPageAllFiles("root", 1000);

        assertThat(keys).containsExactly("root/1.png", "root/2.png");
    }

    @DisplayName("listPageAllFiles caps page size and continues with tokens")
    @Test
    void listPageAllFiles_whenPageSizeTooLarge_capsRequests() throws Exception {
        ListObjectsV2Response only = ListObjectsV2Response.builder()
                .isTruncated(false)
                .contents(List.of())
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(only);

        utils.listPageAllFiles("root", 5000);

        ArgumentCaptor<ListObjectsV2Request> captor = ArgumentCaptor.forClass(ListObjectsV2Request.class);
        verify(s3Client).listObjectsV2(captor.capture());
        assertThat(captor.getValue().maxKeys()).isEqualTo(1000);
    }

    @DisplayName("deleteFile delegates to SDK")
    @Test
    void deleteFile_invokesSdk() throws Exception {
        utils.deleteFile("root/old.png");
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("bucket");
        assertThat(captor.getValue().key()).isEqualTo("root/old.png");
    }

    @DisplayName("batchDeleteFiles splits batches of 1000 keys")
    @Test
    void batchDeleteFiles_handlesChunks() throws Exception {
        List<String> keys = new ArrayList<>();
        keys.add("a");
        keys.add("b");
        keys.add("c");

        utils.batchDeleteFiles(keys);

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client).deleteObjects(captor.capture());
        Delete delete = captor.getValue().delete();
        assertThat(delete.objects()).extracting(ObjectIdentifier::key).containsExactly("a", "b", "c");
    }

    @DisplayName("batchDeleteFiles splits into multiple requests when exceeding 1000 keys")
    @Test
    void batchDeleteFiles_whenMoreThanThousand_splitsBatches() throws Exception {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 1005; i++) {
            keys.add("key-" + i);
        }

        utils.batchDeleteFiles(keys);

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3Client, times(2)).deleteObjects(captor.capture());
        List<DeleteObjectsRequest> requests = captor.getAllValues();
        assertThat(requests.getFirst().delete().objects()).hasSize(1000);
        assertThat(requests.get(1).delete().objects()).hasSize(5);
    }
}
