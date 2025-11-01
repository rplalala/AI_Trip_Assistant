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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @Test
    void getImgUrls_capsMaximumCount() {
        String body = """
                {"results":[{"urls":{"raw":"https://images.example/one"}}]}
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

        utils.getImgUrls("Kyoto", 50);

        assertThat(capturedUri.get().getQuery()).contains("per_page=10");
    }

    @Test
    void getImgUrls_whenCountLessThanOne_throws() {
        UnsplashImgUtils utils = new UnsplashImgUtils(WebClient.builder().exchangeFunction(request -> Mono.empty()), "key");

        assertThatThrownBy(() -> utils.getImgUrls("Tokyo", 0, 100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than 0");
    }

    @Test
    void getImgUrls_whenQueryBlank_throws() {
        UnsplashImgUtils utils = new UnsplashImgUtils(WebClient.builder().exchangeFunction(request -> Mono.empty()), "key");

        assertThatThrownBy(() -> utils.getImgUrls(" ", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void getImgUrls_whenUnsplashReturnsEmpty_throws() {
        String body = """
                {"results":[]}
                """;
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json")
                .body(body)
                .build());
        UnsplashImgUtils utils = new UnsplashImgUtils(WebClient.builder().exchangeFunction(exchangeFunction), "key");

        assertThatThrownBy(() -> utils.getImgUrls("Osaka", 2))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Unsplash has no result");
    }
}

