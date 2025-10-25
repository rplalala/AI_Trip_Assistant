package com.demo.api.client;

import com.fasterxml.jackson.core.JsonProcessingException;

public interface OpenAiClient {

    String requestTripPlan(String prompt);

    <T> T parseContent(String openAiJson, Class<T> clazz);
}

