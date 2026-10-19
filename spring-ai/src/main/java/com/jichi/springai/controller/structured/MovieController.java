package com.jichi.springai.controller.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/movie")
public class MovieController {

    record MovieRecommendation(
            String title,
            String director,
            int year,
            String genre,
            String reason
    ) {
    }

    private final ChatClient chatClient;

    public MovieController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/recommend")
    public MovieRecommendation recommend() {
        return chatClient.prompt()
                .user("推荐一部经典科幻电影")
                .call()
                .entity(MovieRecommendation.class);
    }
}