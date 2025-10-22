package com.demo.api.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class UnsplashImgUtilsTest {

    @Autowired
    private UnsplashImgUtils unsplashImgUtils;

    @Test
    void testUnsplash() {
        System.out.println(unsplashImgUtils.getImgUrls("Sydney", 1));
        System.out.println(unsplashImgUtils.getImgUrls("Sydney", 3, 500, 500));
    }
}
