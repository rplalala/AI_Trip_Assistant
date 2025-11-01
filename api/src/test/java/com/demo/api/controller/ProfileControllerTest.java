package com.demo.api.controller;

import com.demo.api.dto.DeleteAccountDTO;
import com.demo.api.dto.ProfileDTO;
import com.demo.api.dto.UpdatePasswordDTO;
import com.demo.api.exception.BusinessException;
import com.demo.api.exception.GlobalExceptionHandler;
import com.demo.api.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProfileControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProfileController(userService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private RequestPostProcessor withUser(String userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(userId, null));
            request.setUserPrincipal(() -> userId);
            return request;
        };
    }

    @Test
    void get_profile_returns_profile_dto() throws Exception {
        ProfileDTO dto = new ProfileDTO("demo", 32, 1, "demo@example.com", "avatar.png");
        when(userService.getProfileDetail(5L)).thenReturn(dto);

        mockMvc.perform(get("/api/users/profile").with(withUser("5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("demo"))
                .andExpect(jsonPath("$.data.age").value(32));

        verify(userService).getProfileDetail(5L);
    }

    @Test
    void update_profile_delegates_to_service() throws Exception {
        ProfileDTO update = new ProfileDTO("updated", 30, 2, "ignored@example.com", "avatar.png");

        mockMvc.perform(put("/api/users/profile")
                        .with(withUser("9"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        ArgumentCaptor<ProfileDTO> captor = ArgumentCaptor.forClass(ProfileDTO.class);
        verify(userService).updateProfileDetail(eq(9L), captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("updated");
    }

    @Test
    void update_password_rejects_identical_values() throws Exception {
        UpdatePasswordDTO dto = new UpdatePasswordDTO("secret", "secret");

        mockMvc.perform(put("/api/users/profile/pd")
                        .with(withUser("4"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("New password cannot be the same as old password"));
    }

    @Test
    void update_password_invokes_service() throws Exception {
        UpdatePasswordDTO dto = new UpdatePasswordDTO("oldPass", "newPass1");

        mockMvc.perform(put("/api/users/profile/pd")
                        .with(withUser("4"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(userService).updatePassword(4L, dto);
    }

    @Test
    void delete_user_delegates_to_service() throws Exception {
        DeleteAccountDTO dto = new DeleteAccountDTO();
        dto.setVerifyPassword("secret");

        mockMvc.perform(delete("/api/users/profile")
                        .with(withUser("7"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(userService).deleteUser(7L, dto);
    }

    @Test
    void change_email_propagates_business_exception() throws Exception {
        doThrow(new BusinessException("email exists"))
                .when(userService).changeEmail("token-123", "dup@example.com");

        mockMvc.perform(post("/api/users/profile/change-email")
                        .with(withUser("7"))
                        .param("token", "token-123")
                        .param("newEmail", "dup@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("email exists"));
    }
}
