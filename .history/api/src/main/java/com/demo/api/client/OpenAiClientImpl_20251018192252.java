package com.demo.api.client;

import java.util.List;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the OpenAiClient interface using RestTemplate
 * to communicate with the OpenAI Chat Completions API.
 */
@Component
public class OpenAiClientImpl implements OpenAiClient {

    // Logger for debugging and monitoring
    private static final Logger log = LoggerFactory.getLogger(OpenAiClientImpl.class);

    // OpenAI Chat Completions endpoint
    private static final String CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";

    private final String apiKey;             // API key for OpenAI (injected from application.properties/yaml)
    private final RestTemplate restTemplate; // Spring's HTTP client for sending requests
    private final ObjectMapper objectMapper; // Jackson object mapper for JSON serialization/deserialization

    /**
     * Constructor with dependency injection
     */
    public OpenAiClientImpl(@Value("${openai.api-key:}") String apiKey,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
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
                "gpt-4o-mini",    // Model name to use
                0.7,              // Temperature for creativity control
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
            ResponseEntity<String> response = restTemplate.postForEntity(CHAT_COMPLETIONS_URL, entity, String.class);

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