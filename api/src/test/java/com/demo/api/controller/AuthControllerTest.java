package com.demo.api.controller;

import com.demo.api.dto.LoginDTO;
import com.demo.api.dto.RegisterDTO;
import com.demo.api.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();
    }

    @DisplayName("POST /api/register delegates to AuthService")
    @Test
    void register_invokesServiceAndReturnsSuccess() throws Exception {
        RegisterDTO dto = new RegisterDTO();
        dto.setEmail("new@example.com");
        dto.setUsername("new-user");
        dto.setPassword("secret123");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        ArgumentCaptor<RegisterDTO> captor = ArgumentCaptor.forClass(RegisterDTO.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
    }

    @DisplayName("POST /api/login returns JWT on success")
    @Test
    void login_returnsJwt() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setEmail("user@example.com");
        dto.setPassword("secret");
        when(authService.login(dto)).thenReturn("jwt-token");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("jwt-token"));

        verify(authService).login(dto);
    }

    @DisplayName("GET /api/verify-email returns token produced by service")
    @Test
    void verifyEmail_returnsJwt() throws Exception {
        when(authService.verifyEmailByToken("token123")).thenReturn("jwt-verified");

        mockMvc.perform(get("/api/verify-email").param("token", "token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("jwt-verified"));

        verify(authService).verifyEmailByToken("token123");
    }

    @DisplayName("POST /api/forgot-password invokes sendForgotPasswordEmail")
    @Test
    void forgotPassword_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/forgot-password").param("email", "reset@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(authService).sendForgotPasswordEmail("reset@example.com");
    }

    @DisplayName("POST /api/reset-password forwards token and password")
    @Test
    void resetPassword_delegatesToService() throws Exception {
        mockMvc.perform(post("/api/reset-password")
                        .param("token", "abc")
                        .param("newPassword", "new-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(authService).resetPassword("abc", "new-secret");
    }

    @DisplayName("POST /api/resend-verify-email delegates to service")
    @Test
    void resendVerifyEmail_callsService() throws Exception {
        mockMvc.perform(post("/api/resend-verify-email").param("email", "again@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        verify(authService).resendVerifyEmail("again@example.com");
    }
}
