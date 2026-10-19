package com.jichi.prompt.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceController {

    private final ChatClient customerServiceClient;

    public CustomerServiceController(
            @Qualifier("customerAiServiceClient") ChatClient customerServiceClient) {
        this.customerServiceClient = customerServiceClient;
    }

    @GetMapping
    public String ask(@RequestParam String question) {
        return customerServiceClient.prompt()
                .user(question)
                .call()
                .content();
    }
}