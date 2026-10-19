package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.constant.SecurityBoundaryPrompts;
import com.jichi.prompt.entity.FilterResult;
import com.jichi.prompt.service.OutputContentFilter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/safe-output")
public class SafeOutputController {

    private final OutputContentFilter outputFilter;
    private final ChatClient chatClient;

    public SafeOutputController(OutputContentFilter outputFilter,
                                 DashScopeChatModel chatModel) {
        this.outputFilter = outputFilter;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SecurityBoundaryPrompts.CONTENT_BOUNDARY)
                .build();
    }

    record AskRequest(String message) {}

    @PostMapping("/ask")
    public ResponseEntity<String> ask(@RequestBody AskRequest req) {
        // 先让模型回答
        String reply = chatClient.prompt()
                .user(req.message())
                .call()
                .content();

        // 再对模型输出过滤
        FilterResult result = outputFilter.filter(reply);
        if (!result.safe()) {
            return ResponseEntity.badRequest().body("输出被拦截：" + result.reason());
        }

        return ResponseEntity.ok(reply);
    }
}