package com.jichi.springaialibaba.controller.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/business")
public class BusinessController {

    private final ChatClient customerServiceChatClient;
    private final ChatClient contentChatClient;

    public BusinessController(
            @Qualifier("customerServiceChatClient") ChatClient customerServiceChatClient,
            @Qualifier("contentChatClient") ChatClient contentChatClient) {
        this.customerServiceChatClient = customerServiceChatClient;
        this.contentChatClient = contentChatClient;
    }

    @GetMapping("/customer-service")
    public String customerService(@RequestParam String message) {
        return customerServiceChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/generate-copy")
    public String generateCopy(@RequestParam String brief) {
        return contentChatClient.prompt()
                .user("根据以下简报，生成一段产品推广文案：\n" + brief)
                .call()
                .content();
    }
}