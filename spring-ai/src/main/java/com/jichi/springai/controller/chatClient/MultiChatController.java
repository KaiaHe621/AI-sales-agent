package com.jichi.springai.controller.chatClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/multi-chat")
public class MultiChatController {

    private final ChatClient customerServiceChatClient;
    private final ChatClient codingChatClient;

    public MultiChatController(
            @Qualifier("customerServiceChatClient") ChatClient customerServiceChatClient,
            @Qualifier("codingChatClient") ChatClient codingChatClient) {
        this.customerServiceChatClient = customerServiceChatClient;
        this.codingChatClient = codingChatClient;
    }

    @GetMapping("/service")
    public String service(@RequestParam String message) {
        return customerServiceChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/code")
    public String code(@RequestParam String message) {
        return codingChatClient.prompt()
                .user(message)
                .call()
                .content();
    }

}