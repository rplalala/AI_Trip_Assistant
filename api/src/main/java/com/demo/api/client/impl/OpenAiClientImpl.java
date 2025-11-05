package com.demo.api.client.impl;

import com.demo.api.client.OpenAiClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Implementation of the OpenAiClient interface using RestTemplate
 * to communicate with the OpenAI Chat Completions API.
 */
@Component
public class OpenAiClientImpl implements OpenAiClient {
    // Logger for debugging and monitoring
    private static final Logger log = LoggerFactory.getLogger(OpenAiClientImpl.class);

    private final String apiKey;             // API key for OpenAI (injected from application.properties/yaml)
    private final String modelName;         // Model name to use
    private final double temperature;       // Temperature for creativity control
    private final String chatCompletionsUrl; // OpenAI Chat Completions endpoint

    private final RestTemplate restTemplate; // Spring's HTTP client for sending requests
    private final ObjectMapper objectMapper; // Jackson object mapper for JSON serialization/deserialization

    /**
     * Constructor with dependency injection
     */
    public OpenAiClientImpl(@Value("${spring.ai.openai.api-key:}") String apiKey,
                            @Value("${spring.ai.openai.model-name:gpt-4o-mini}") String modelName,
                            @Value("${spring.ai.openai.temperature:0.7}") double temperature,
                            @Value("${spring.ai.openai.chat-completions-url:https://api.openai.com/v1/chat/completions}") String chatCompletionsUrl,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.temperature = temperature;
        this.chatCompletionsUrl = chatCompletionsUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls the OpenAI Chat Completions API with a prompt and returns the raw JSON response as a string.
     */
    @Override
    public String requestTripPlan(String prompt) {
        // Ensure the API key is set
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OpenAI API key is not configured");
        }

        // Ensure the prompt is not empty
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt must not be empty");
        }

        // Construct request payload with model, temperature, and message history
        ChatCompletionRequest payload = new ChatCompletionRequest(
                modelName,
                temperature,
                List.of(
                        new ChatMessage("system", "You are a helpful travel planner."), // System instruction
                        new ChatMessage("user", prompt)                                 // User's travel prompt
                )
        );

        try {
            // Convert the payload to JSON
            String requestJson = objectMapper.writeValueAsString(payload);
            log.debug("Sending OpenAI chat completion request: {}", requestJson);

            // Set request headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);  // Indicate JSON format
            headers.setBearerAuth(apiKey);                       // Set Bearer token for authorization

            // Create an HTTP request with body and headers
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            // Send the POST request to OpenAI
            ResponseEntity<String> response = restTemplate.postForEntity(chatCompletionsUrl, entity, String.class);

            // Extract response body
            String body = response.getBody();
            log.debug("Received OpenAI response (status={}): {}", response.getStatusCode(), body);

            // Check if the response is successful
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("OpenAI API returned non-success status: " + response.getStatusCode());
            }

            // Check if the response body is empty
            if (!StringUtils.hasText(body)) {
                throw new IllegalStateException("OpenAI API returned an empty response body");
            }

            // Return the raw JSON string from OpenAI
            return body;

        } catch (JsonProcessingException ex) {
            // JSON serialization failed
            throw new IllegalStateException("Failed to serialize OpenAI chat completion request", ex);
        } catch (RestClientException ex) {
            // HTTP request failed
            throw new IllegalStateException("Failed to call OpenAI API", ex);
        }
    }

    /**
     * Parses the inner "content" JSON from an OpenAI ChatCompletion response
     * into the specified target type.
     *
     * @param openAiJson The full JSON string returned by the OpenAI API
     * @param clazz      The target class type for deserialization (e.g., InsightResponse.class)
     * @return Deserialized object of type T
     */
    @Override
    public <T> T parseContent(String openAiJson, Class<T> clazz) {
        log.info("OpenAiClient parseContent start: {}", openAiJson);

        try {
            // Parse the outer OpenAI JSON
            JsonNode root = objectMapper.readTree(openAiJson);
            String contentJson = root.path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

            // Clean Markdown code fences if present
            contentJson = cleanJsonString(contentJson);

            // Deserialize the "content" field into the target type
            return objectMapper.readValue(contentJson, clazz);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse OpenAI response", e);
            return null;
        }
    }

    private static String cleanJsonString(String content) {
        if (content == null) return null;

        // Remove Markdown code fences like ```json ... ``` or ``` ... ```
        content = content.trim();
        if (content.startsWith("```")) {
            // Find the first '{' or '[' after the code fence
            int firstBrace = content.indexOf('{');
            int firstBracket = content.indexOf('[');
            int start = (firstBrace >= 0 && firstBracket >= 0)
                    ? Math.min(firstBrace, firstBracket)
                    : Math.max(firstBrace, firstBracket);

            int endBrace = Math.max(content.lastIndexOf('}'), content.lastIndexOf(']'));
            if (start >= 0 && endBrace > start) {
                content = content.substring(start, endBrace + 1);
            }
        }

        // Remove invisible control characters (safety)
        return content.replaceAll("[\\u0000-\\u001F]", "").trim();
    }

    /**
     * Represents the request payload for OpenAI Chat Completions API.
     */
    private record ChatCompletionRequest(String model, double temperature, List<ChatMessage> messages) {
    }

    /**
     * Represents a single chat message for the OpenAI API.
     */
    private record ChatMessage(String role, String content) {
    }
}