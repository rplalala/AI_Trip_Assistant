package com.demo.api.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Unsplash Image Utils
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnsplashImgUtils {

    private final WebClient unsplashWebClient;

    /**
     * Search N image urls.
     * @param q search keyword
     * @param n number of images to be returned
     * @param width width of the image
     * @param height height of the image
     * @return a list of Aws S3 URLs
     */
    public List<String> getImgUrls(String q, int n, int width, int height) {
        return searchNImg(q, n).stream()
                .map(photo -> imgUrlFormat(photo, width, height)).toList();
    }

    /**
     * Search N image urls (default size: 1600x900).
     * @param q search keyword
     * @param n number of images to be returned
     * @return a list of Aws S3 URLs
     */
    public List<String> getImgUrls(String q, int n){
        return searchNImg(q, n).stream()
                .map(photo -> imgUrlFormat(photo, 1600, 900)).toList();
    }

    /**
     * Get N image Details (JSON) from Unsplash API.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> searchNImg(String query, int n) {
        if(n > 10){
            n = 10;
            log.warn("maximum number is 10, automatically adjusted to 10");
        }
        if(n < 1){
            throw new IllegalArgumentException("number must be greater than 0");
        }
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        final int count = n;
        Map<String, Object> body = unsplashWebClient.get()
                .uri(uri -> uri.path("/search/photos")
                        .queryParam("query", query)
                        .queryParam("per_page", count)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) (body != null ? body.getOrDefault("results", List.of()) : List.of());

        if (results.isEmpty()) {
            throw new NoSuchElementException("Unsplash has no result for: " + query);
        }
        return results;
    }

    /**
     * Generate url and format the img.
     */
    @SuppressWarnings("unchecked")
    private String imgUrlFormat(Map<String, Object> photo, int width, int height) {
        Map<String, Object> urls = (Map<String, Object>) photo.get("urls");
        String base = (String) (urls.getOrDefault("raw",
                urls.getOrDefault("full", urls.get("regular"))));
        if (base == null || base.isBlank()) {
            throw new IllegalStateException("No downloadable url in photo.urls");
        }

        String join = base.contains("?") ? "&" : "?";
        // q: compress img quality 1-100
        return base + join + "w=" + width + "&h=" + height + "&fit=crop&fm=jpg&q=80&auto=format";
    }
}
