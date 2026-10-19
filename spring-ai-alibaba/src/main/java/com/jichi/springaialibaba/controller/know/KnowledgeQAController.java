package com.jichi.springaialibaba.controller.know;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qa")
public class KnowledgeQAController {

    private final ChatClient ragChatClient;

    public KnowledgeQAController(@Qualifier("ragChatClient") ChatClient ragChatClient) {
        this.ragChatClient = ragChatClient;
    }

    @GetMapping
    public String ask(@RequestParam String question) {
        return ragChatClient.prompt()
                .user(question)
                .call()
                .content();
    }

    // 只在指定分类里检索，适合多租户或权限控制场景
    @GetMapping("/category")
    public String askWithCategory(
            @RequestParam String question,
            @RequestParam String category) {

        FilterExpressionBuilder fb = new FilterExpressionBuilder();
        var filter = fb.eq("category", category).build();

        return ragChatClient.prompt()
                .user(question)
                .advisors(a -> a.param(
                        QuestionAnswerAdvisor.FILTER_EXPRESSION, filter.toString()))
                .call()
                .content();
    }
}