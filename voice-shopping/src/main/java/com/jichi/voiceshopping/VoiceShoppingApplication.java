package com.jichi.voiceshopping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableCaching
public class VoiceShoppingApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceShoppingApplication.class, args);
    }
}