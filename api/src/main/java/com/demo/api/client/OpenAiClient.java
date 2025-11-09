package com.demo.api.client;

public interface OpenAiClient {

    /**
     * Generates a structured response for the given prompt and maps it to the provided DTO type.
     *
     * @param prompt       fully formatted user prompt
     * @param responseType target DTO type to deserialize into
     * @return mapped DTO
     */
    <T> T generate(String prompt, Class<T> responseType);
}

