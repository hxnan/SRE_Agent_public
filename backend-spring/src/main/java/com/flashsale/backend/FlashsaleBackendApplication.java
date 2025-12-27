package com.flashsale.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashsaleBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlashsaleBackendApplication.class, args);
    }
}

