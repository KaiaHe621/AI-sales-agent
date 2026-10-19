package com.jichi.springai.controller.structured;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/article")
public class ArticleAnalysisController {

    private final ChatClient chatClient;

    public ArticleAnalysisController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一个专业的文章分析助手。
                        分析准确，不要添加原文没有的内容。
                        """)
                .build();
    }

    // 文章分析结果
    record ArticleAnalysis(
            @JsonPropertyDescription("文章标题，如果没有则根据内容生成")
            String title,

            @JsonPropertyDescription("文章类型：NEWS/OPINION/TUTORIAL/RESEARCH/OTHER")
            String type,

            @JsonPropertyDescription("100字以内的摘要")
            String summary,

            @JsonPropertyDescription("关键词列表，最多5个")
            List<String> keywords,

            @JsonPropertyDescription("文章的主要观点，最多3条")
            List<String> mainPoints,

            @JsonPropertyDescription("情感倾向：POSITIVE/NEGATIVE/NEUTRAL")
            String sentiment,

            @JsonPropertyDescription("可读性评分，1-10分，10分最易读")
            int readabilityScore
    ) {
    }

    /**
     * 分析文章
     */
    @PostMapping("/analyze")
    public ArticleAnalysis analyze(@RequestBody ArticleRequest request) {
        return chatClient.prompt()
                .user("请分析以下文章：\n\n" + request.content())
                .call()
                .entity(ArticleAnalysis.class);
    }

    /**
     * 批量提取关键词
     */
    @PostMapping("/keywords")
    public List<String> extractKeywords(@RequestBody ArticleRequest request) {
        return chatClient.prompt()
                .user("从以下文章中提取5个最重要的关键词：\n\n" + request.content())
                .call()
                .entity(new ParameterizedTypeReference<List<String>>() {
                });
    }

    record ArticleRequest(String content) {
    }
}