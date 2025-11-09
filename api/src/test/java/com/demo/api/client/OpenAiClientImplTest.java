package com.demo.api.client;

import com.demo.api.client.impl.OpenAiClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class OpenAiClientImplTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;

    private OpenAiClientImpl client;

    @BeforeEach
    void setUp() {
        client = new OpenAiClientImpl(chatClient, 1, Duration.ZERO, "system prompt");
    }

    @Test
    void generate_returnsMappedEntity() {
        TestDto dto = new TestDto("Tokyo");
        when(chatClient.prompt().system(anyString()).user(anyString()).call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(TestDto.class)).thenReturn(dto);

        TestDto result = client.generate("Plan a trip", TestDto.class);

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void generate_validatesInputs() {
        assertThatThrownBy(() -> client.generate("   ", TestDto.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt");
        assertThatThrownBy(() -> client.generate("prompt", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Response type");
    }

    @Test
    void generate_whenResponseNull_throwsIllegalState() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()).thenReturn(null);

        assertThatThrownBy(() -> client.generate("prompt", TestDto.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void generate_whenMappingFails_throwsIllegalState() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(TestDto.class)).thenReturn(null);

        assertThatThrownBy(() -> client.generate("prompt", TestDto.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mapped");
    }

    @Test
    void generate_retriesAndPropagatesFailure() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call()).thenThrow(new IllegalStateException("boom"));

        OpenAiClientImpl retryingClient = new OpenAiClientImpl(chatClient, 2, Duration.ZERO, "system prompt");

        assertThatThrownBy(() -> retryingClient.generate("prompt", TestDto.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
    }

    private record TestDto(String city) {}
}
