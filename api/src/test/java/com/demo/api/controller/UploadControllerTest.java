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
import static org.mockito.Mockito.verify;
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
}
