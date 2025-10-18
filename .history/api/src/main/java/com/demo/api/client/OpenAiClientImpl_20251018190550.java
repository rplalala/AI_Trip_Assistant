package com.demo.api.client;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * OpenAI client backed by the Chat Completions HTTP API.
 */
@Component
@RequiredArgsConstructor
public class OpenAiClientImpl implements OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClientImpl.class);
    private static final URI CHAT_COMPLETIONS_URI = URI.create("https://api.openai.com/v1/chat/completions");
    private static final String MODEL_NAME = "gpt-4-1106-preview";
    private static final double TEMPERATURE = 0.7d;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Override
    public String requestTripPlan(String prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        ChatCompletionRequest requestPayload = new ChatCompletionRequest(
                MODEL_NAME,
                TEMPERATURE,
                List.of(
                        new ChatMessage("system", "You are a helpful travel planner."),
                        new ChatMessage("user", prompt)
                )
        );

        try {
            String requestJson = objectMapper.writeValueAsString(requestPayload);
            log.debug("Sending OpenAI Chat Completions request: {}", requestJson);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.exchange(CHAT_COMPLETIONS_URI, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("OpenAI API returned status: " + response.getStatusCode());
            }

            String responseBody = response.getBody();
            log.debug("Received OpenAI Chat Completions response: {}", responseBody);
            return responseBody;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize OpenAI request payload", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("OpenAI API call failed", ex);
        }
    }

    private static final class ChatCompletionRequest {
        private final String model;
        private final double temperature;
        private final List<ChatMessage> messages;

        private ChatCompletionRequest(String model, double temperature, List<ChatMessage> messages) {
            this.model = model;
            this.temperature = temperature;
            this.messages = messages;
        }

        public String getModel() {
            return model;
        }

        public double getTemperature() {
            return temperature;
        }

        public List<ChatMessage> getMessages() {
            return messages;
        }
    }

    private static final class ChatMessage {
        private final String role;
        private final String content;

        private ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}

