package com.demo.api.controller;

import com.demo.api.exception.BusinessException;
import com.demo.api.service.UserService;
import com.demo.api.utils.AwsS3Utils;
import com.demo.api.ApiRespond;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UploadControllerTest {

    @Mock
    private AwsS3Utils awsS3Utils;
    @Mock
    private UserService userService;

    @InjectMocks
    private UploadController uploadController;

    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        file = new MockMultipartFile("file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes());
    }

    @Test
    void upload_returnsCdnUrl() throws Exception {
        when(awsS3Utils.upload(any(), any(), any(Boolean.class))).thenReturn("https://cdn.example.com/photo.jpg");

        ApiRespond<String> response = uploadController.upload(file);

        assertThat(response.getData()).isEqualTo("https://cdn.example.com/photo.jpg");
    }

    @Test
    void uploadAvatar_updatesUserAvatar() throws Exception {
        when(awsS3Utils.upload(any(), any(), any(Boolean.class))).thenReturn("https://cdn.example.com/avatar.png");

        ApiRespond<String> response = uploadController.uploadAvatar(file, "42");

        assertThat(response.getData()).isEqualTo("https://cdn.example.com/avatar.png");
        verify(userService).updateAvatar(42L, "https://cdn.example.com/avatar.png");
    }

    @Test
    void upload_whenFileEmpty_throwsBusinessException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]);

        assertThatThrownBy(() -> uploadController.upload(emptyFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File dose not exist");
    }

    @Test
    void uploadAvatar_whenFileTooLarge_throwsBusinessException() {
        byte[] largeContent = new byte[11 * 1024 * 1024];
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.jpg", MediaType.IMAGE_JPEG_VALUE, largeContent);

        assertThatThrownBy(() -> uploadController.uploadAvatar(largeFile, "99"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File too large");
    }

    @Test
    void upload_whenFileTooLarge_throwsBusinessException() {
        byte[] largeContent = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile largeFile = new MockMultipartFile("file", "poster.png", MediaType.IMAGE_PNG_VALUE, largeContent);

        assertThatThrownBy(() -> uploadController.upload(largeFile))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File too large");
    }

    @Test
    void uploadByUrl_whenBlank_throwsBusinessException() {
        assertThatThrownBy(() -> uploadController.uploadByUrl(""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("URL cannot be empty");
    }

    @Test
    void uploadByUrl_returnsUploadedUrl() throws Exception {
        when(awsS3Utils.uploadFromUrl(eq("https://example.com/pic.jpg"), eq(false)))
                .thenReturn("https://cdn/pic.jpg");

        ApiRespond<String> response = uploadController.uploadByUrl("https://example.com/pic.jpg");

        assertThat(response.getData()).isEqualTo("https://cdn/pic.jpg");
    }

    @Test
    void uploadAvatarFromUrl_whenBlank_throwsBusinessException() {
        assertThatThrownBy(() -> uploadController.uploadAvatarFrmUrl("", "88"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("URL cannot be empty");
        verifyNoInteractions(userService);
    }

    @Test
    void uploadAvatarFromUrl_updatesUserAvatar() throws Exception {
        when(awsS3Utils.uploadFromUrl(eq("https://cdn/avatar.png"), eq(true)))
                .thenReturn("https://s3/avatar.png");

        ApiRespond<String> response = uploadController.uploadAvatarFrmUrl("https://cdn/avatar.png", "77");

        assertThat(response.getData()).isEqualTo("https://s3/avatar.png");
        verify(userService).updateAvatar(77L, "https://s3/avatar.png");
    }
}
