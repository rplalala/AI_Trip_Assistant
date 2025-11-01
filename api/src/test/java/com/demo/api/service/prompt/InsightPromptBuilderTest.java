package com.demo.api.service.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InsightPromptBuilderTest {

    @Test
    void builderClassIsInstantiable() {
        InsightPromptBuilder builder = new InsightPromptBuilder();

        assertThat(builder).isNotNull();
    }
}
