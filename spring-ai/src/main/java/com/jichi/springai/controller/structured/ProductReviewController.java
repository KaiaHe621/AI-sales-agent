package com.jichi.springai.controller.structured;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/review")
public class ProductReviewController {

    record ProductReview(
            @JsonProperty("product_name")
            @JsonPropertyDescription("商品名称，从评论中提取")
            String productName,

            @JsonProperty("sentiment")
            @JsonPropertyDescription("情感倾向：POSITIVE（正面）、NEGATIVE（负面）、NEUTRAL（中性）")
            String sentiment,

            @JsonProperty("score")
            @JsonPropertyDescription("评分，1-5分，根据评论语气推断")
            int score,

            @JsonProperty("key_points")
            @JsonPropertyDescription("评论中提到的关键点，最多3条")
            List<String> keyPoints,

            @JsonProperty("improvement_suggestions")
            @JsonPropertyDescription("改进建议，如果没有则为空列表")
            List<String> improvementSuggestions
    ) {
    }

    record ReviewRequest(String content) {
    }

    private final ChatClient chatClient;

    public ProductReviewController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/analyze")
    public ProductReview analyze(@RequestBody ReviewRequest request) {
        return chatClient.prompt()
                .user("分析这条商品评论：" + request.content())
                .call()
                .entity(ProductReview.class);
    }
}