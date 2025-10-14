package com.demo.externalservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class PingController {
    @GetMapping("/booking/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "ok", true,
                "service", "external-service",
                "random", Math.random());
    }
}
