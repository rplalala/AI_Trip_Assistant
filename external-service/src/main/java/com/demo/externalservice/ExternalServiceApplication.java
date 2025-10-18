package com.demo.externalservice;

import com.demo.externalservice.config.BookingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(BookingProperties.class)
public class ExternalServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExternalServiceApplication.class, args);
	}

}
