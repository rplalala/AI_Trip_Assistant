package com.demo.api.controller;

import com.demo.api.client.BookingClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AITestController {
    private final ChatClient chat;

    private final BookingClient bookingClient;

    public AITestController(ChatClient.Builder builder, BookingClient bookingClient) {
        this.chat = builder.build();
        this.bookingClient = bookingClient;
    }

    @GetMapping("/test")
    public String ping() {
        // Test: let the model say a sentence
        return chat
                .prompt()
                .user(
                        "Say hello, test successful. " +
                                "random introduce a city in the world"
                )
                .call()
                .content();
    }

    // TODO: Need to modify this test after BookingClient is ready
    // @GetMapping("/testexternal")
    // public String testExternal() {
    //     // test connecting External service
    //     return bookingClient.ping();
    // }
}
