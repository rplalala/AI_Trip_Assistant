package com.demo.api.utils;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UnsplashImgUtilsTest {

    @Test
    void getImgUrls_buildsRequestAndFormatsDimensions() {
        String body = """
                {
                  "results": [
                    {"urls":{"raw":"https://images.example/one"}},
                    {"urls":{"full":"https://images.example/two"}}
                  ]
                }
                """;
        AtomicReference<URI> capturedUri = new AtomicReference<>();
        ExchangeFunction exchangeFunction = request -> {
            capturedUri.set(request.url());
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .build());
        };

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        UnsplashImgUtils utils = new UnsplashImgUtils(builder, "test-key");

        List<String> urls = utils.getImgUrls("Kyoto", 2, 400, 250);

        assertThat(capturedUri.get()).hasPath("/search/photos");
        assertThat(capturedUri.get().getQuery()).contains("query=Kyoto").contains("per_page=2");
        assertThat(urls).hasSize(2);
        assertThat(urls.getFirst()).contains("w=400").contains("h=250").contains("fit=crop");
        assertThat(urls.get(1)).contains("w=400").contains("h=250");
    }
}

