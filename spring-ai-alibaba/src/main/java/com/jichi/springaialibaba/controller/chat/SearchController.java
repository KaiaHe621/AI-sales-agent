package com.jichi.springaialibaba.controller.chat;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qwen/search")
public class SearchController {

    private final ChatClient chatClient;

    public SearchController(DashScopeChatModel dashScopeChatModel) {
        this.chatClient = ChatClient.builder(dashScopeChatModel).build();
    }

    @GetMapping
    public String search(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen-max")
                        .withEnableSearch(true)   // 开启联网搜索
                        .build())
                .call()
                .content();
    }

    @GetMapping("/think")
    public String think(@RequestParam String question) {
        return chatClient.prompt()
                .user(question)
                .options(DashScopeChatOptions.builder()
                        .withModel("qwen3-235b-a22b")
                        .withEnableThinking(true)
                        .withThinkingBudget(2000)   // 思考过程最多用 2000 token
                        .build())
                .call()
                .content();
    }

}