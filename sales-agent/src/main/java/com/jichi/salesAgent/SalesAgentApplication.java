package com.jichi.salesAgent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
public class SalesAgentApplication {
    public static void main(String[] args) {
        SpringApplication.run(SalesAgentApplication.class, args);
    }
}
