package com.demo.api.utils;

import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendGridUtilsTest {

    @Mock
    private SendGrid sendGrid;

    @InjectMocks
    @Spy
    private SendGridUtils utils = new SendGridUtils("api-key");

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(utils, "client", sendGrid);
        ReflectionTestUtils.setField(utils, "from", "noreply@example.com");
    }

    @DisplayName("sendHtml builds request and throws when response status >= 400")
    @Test
    void sendHtml_successAndFailure() throws Exception {
        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        when(sendGrid.api(captor.capture())).thenReturn(new Response(202, "", null));

        utils.sendHtml("user@example.com", "Welcome", "<h1>Hello</h1>");

        Request request = captor.getValue();
        assertThat(request.getEndpoint()).isEqualTo("mail/send");
        assertThat(request.getMethod().name()).isEqualTo("POST");
        assertThat(request.getBody()).contains("user@example.com").contains("Welcome");

        when(sendGrid.api(any(Request.class))).thenReturn(new Response(500, "oops", null));

        assertThatThrownBy(() -> utils.sendHtml("user@example.com", "Bad", "<p>Fail</p>"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email send failed");
    }

    @DisplayName("templated helpers delegate to sendHtml with expected subjects")
    @Test
    void templatedMethodsDelegate() throws Exception {
        doNothing().when(utils).sendHtml(eq("user@example.com"), any(), any());

        utils.sendVerifyEmail("user@example.com", "https://verify");
        verify(utils).sendHtml(eq("user@example.com"), eq("Verify your email"), any());

        utils.sendResetEmail("user@example.com", "https://reset");
        verify(utils).sendHtml(eq("user@example.com"), eq("Reset your password"), any());

        utils.sendChangeEmail("user@example.com", "https://change");
        verify(utils).sendHtml(eq("user@example.com"), eq("Change your email"), any());
    }
}
