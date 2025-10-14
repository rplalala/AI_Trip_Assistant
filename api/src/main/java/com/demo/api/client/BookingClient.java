package com.demo.api.client;

import com.demo.api.config.BookingFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "booking-service",
        url = "http://localhost:8092/api",
        path = "/api/booking",
        configuration = BookingFeignConfig.class
)

public interface BookingClient {
    @GetMapping("/ping")
    String ping();
}
