package com.jichi.springai.controller.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/book")
public class BookController {

    record BookSummary(String title, String author, String oneLinerSummary) {}

    private final ChatClient chatClient;

    public BookController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/list")
    public List<BookSummary> list() {
        return chatClient.prompt()
                .user("列出 5 本经典的 Java 技术书籍")
                .call()
                .entity(new ParameterizedTypeReference<List<BookSummary>>() {});
    }
}