package com.demo.api.utils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AliyunOSSTest {

    @Autowired
    private AliyunOSSUtils ossUtils;

    @Test
    void name() throws Exception {
        ossUtils.uploadFromUrl("https://awscdn.dingzh.cc/elec5620-stage2/avatars/default_avatar.png", true);

    }
}
