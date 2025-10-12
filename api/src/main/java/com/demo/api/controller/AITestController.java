package com.demo.api.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AITestController {
    private final ChatClient chat;

    public AITestController(ChatClient.Builder builder) {
        this.chat = builder.build();
    }

    @GetMapping("/test")
    public String ping() {
        // 测试让模型说一句话
        return chat
                .prompt()
                .user(
                        "Say hello, test successful. " +
                                "random introduce a city in the world"
                )
                .call()
                .content();
    }
}
