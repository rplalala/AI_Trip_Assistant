package com.demo.api.client.impl;

import com.demo.api.client.OpenAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;

@Component
public class OpenAiClientImpl implements OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClientImpl.class);
    private static final String DEFAULT_SYSTEM_PROMPT =
            "You are a helpful travel planner. Return only strict JSON that matches the requested schema. "
                    + "Never include markdown code fences or natural language outside of the JSON payload.";

    private final ChatClient chatClient;
    private final int maxAttempts;
    private final Duration retryBackoff;
    private final String systemPrompt;

    public OpenAiClientImpl(ChatClient chatClient,
                            @Value("${app.openai.retry.max-attempts:3}") int maxAttempts,
                            @Value("${app.openai.retry.backoff:PT0.5S}") Duration retryBackoff,
                            @Value("${app.openai.system-prompt:}") String systemPrompt) {
        this.chatClient = chatClient;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.retryBackoff = retryBackoff == null ? Duration.ofMillis(500) : retryBackoff;
        this.systemPrompt = StringUtils.hasText(systemPrompt) ? systemPrompt : DEFAULT_SYSTEM_PROMPT;
    }

    @Override
    public <T> T generate(String prompt, Class<T> responseType) {
        Assert.hasText(prompt, "Prompt must not be empty");
        Assert.notNull(responseType, "Response type must not be null");

        IllegalStateException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ChatClient.CallResponseSpec responseSpec = chatClient.prompt()
                        .system(systemPrompt)
                        .user(prompt)
                        .call();

                if (responseSpec == null) {
                    throw new IllegalStateException("OpenAI returned an empty response");
                }

                T mapped = responseSpec.entity(responseType);
                if (mapped == null) {
                    throw new IllegalStateException("OpenAI response could not be mapped to " + responseType.getSimpleName());
                }

                return mapped;
            } catch (RuntimeException ex) {
                log.warn("OpenAI call failed on attempt {}/{}: {}", attempt, maxAttempts, ex.getMessage());
                lastFailure = (ex instanceof IllegalStateException) ? (IllegalStateException) ex
                        : new IllegalStateException("Failed to call OpenAI", ex);
                if (attempt == maxAttempts) {
                    throw lastFailure;
                }
                sleepBackoff(attempt);
            }
        }

        throw lastFailure != null ? lastFailure : new IllegalStateException("Failed to call OpenAI");
    }

    private void sleepBackoff(int attempt) {
        long delay = retryBackoff.multipliedBy(Math.max(1, attempt)).toMillis();
        if (delay <= 0) {
            return;
        }
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying OpenAI call", e);
        }
    }
}
