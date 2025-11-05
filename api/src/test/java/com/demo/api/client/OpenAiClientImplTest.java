package com.demo.api.client;

import com.demo.api.client.impl.OpenAiClientImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiClientImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OpenAiClientImpl client = new OpenAiClientImpl("api-key", "gpt-40-mini", 0.7, "http://api-mockup", restTemplate, objectMapper);

    @BeforeEach
    void setUp() {
        client = new OpenAiClientImpl("api-key", "gpt-40-mini", 0.7, "http://api-mockup", restTemplate, objectMapper);
    }

    @DisplayName("requestTripPlan posts payload and returns response body")
    @Test
    void requestTripPlan_successfulCall() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"prompt\":\"value\"}");
        ResponseEntity<String> response = ResponseEntity.ok("{\"id\":\"req\"}");
        when(restTemplate.postForEntity(eq("https://api.openai.com/v1/chat/completions"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        String result = client.requestTripPlan("Plan a trip to Tokyo");

        assertThat(result).isEqualTo("{\"id\":\"req\"}");

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq("https://api.openai.com/v1/chat/completions"), captor.capture(), eq(String.class));
        HttpEntity<String> entity = captor.getValue();
        assertThat(entity.getBody()).isEqualTo("{\"prompt\":\"value\"}");
        HttpHeaders headers = entity.getHeaders();
        assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer api-key");
    }

    @DisplayName("requestTripPlan requires configured API key and non-empty prompt")
    @Test
    void requestTripPlan_validatesInputs() {
        OpenAiClientImpl noKeyClient = new OpenAiClientImpl("api-key", "gpt-40-mini", 0.7, "http://api-mockup", restTemplate, objectMapper);
        assertThatThrownBy(() -> noKeyClient.requestTripPlan("anything"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("API key");

        assertThatThrownBy(() -> client.requestTripPlan("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prompt must not be empty");
    }

    @DisplayName("requestTripPlan wraps serialization and HTTP failures")
    @Test
    void requestTripPlan_handlesFailures() throws Exception {
        Mockito.doThrow(new JsonProcessingException("boom") {})
                .when(objectMapper).writeValueAsString(any());

        assertThatThrownBy(() -> client.requestTripPlan("Plan a trip"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize");

        Mockito.doReturn("{}").when(objectMapper).writeValueAsString(any());
        when(restTemplate.postForEntity(any(String.class), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("network down"));

        assertThatThrownBy(() -> client.requestTripPlan("Plan a trip"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to call OpenAI API");
    }

    @DisplayName("requestTripPlan detects non-2xx responses")
    @Test
    void requestTripPlan_whenStatusNonSuccess_throwsIllegalState() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        ResponseEntity<String> response = ResponseEntity.status(500).body("{\"error\":\"fail\"}");
        when(restTemplate.postForEntity(eq("https://api.openai.com/v1/chat/completions"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> client.requestTripPlan("Describe Sydney"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-success status");
    }

    @DisplayName("requestTripPlan ensures non-empty response body")
    @Test
    void requestTripPlan_whenBodyEmpty_throwsIllegalState() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        ResponseEntity<String> response = ResponseEntity.ok("");
        when(restTemplate.postForEntity(eq("https://api.openai.com/v1/chat/completions"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(response);

        assertThatThrownBy(() -> client.requestTripPlan("Trip to Rome"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("empty response body");
    }

    @DisplayName("parseContent extracts JSON even when wrapped in code fences")
    @Test
    void parseContent_extractsAndDeserializes() throws Exception {
        String response = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "```json\\n{\\n  \\"title\\": \\"Tokyo\\"\\n}\\n```"
                      }
                    }
                  ]
                }
                """;

        when(objectMapper.readTree(response)).thenReturn(new ObjectMapper().readTree(response));
        when(objectMapper.readValue(any(String.class), eq(TestDto.class)))
                .thenReturn(new TestDto("Tokyo"));

        TestDto dto = client.parseContent(response, TestDto.class);

        assertThat(dto).isNotNull();
        assertThat(dto.title()).isEqualTo("Tokyo");
    }

    @DisplayName("parseContent returns null on parsing errors")
    @Test
    void parseContent_onFailureReturnsNull() throws Exception {
        String response = "{\"choices\":[]}";
        when(objectMapper.readTree(response)).thenThrow(new JsonProcessingException("boom") {});

        assertThat(client.parseContent(response, TestDto.class)).isNull();
    }

    private record TestDto(String title) {}
}
