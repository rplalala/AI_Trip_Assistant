package com.demo.api.controller;

import com.demo.api.client.BookingClient;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AITestControllerTest {

    @Mock
    private ChatClient.Builder builder;
    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec promptSpec;
    @Mock
    private ChatClient.CallResponseSpec responseSpec;
    @Mock
    private BookingClient bookingClient;

    @Test
    void ping_delegatesToChatClient() {
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(anyString())).thenReturn(promptSpec);
        when(promptSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("hello world");

        AITestController controller = new AITestController(builder, bookingClient);

        String result = controller.ping();

        assertThat(result).isEqualTo("hello world");
        verify(promptSpec).user(contains("Say hello"));
        verify(responseSpec).content();
    }
}
