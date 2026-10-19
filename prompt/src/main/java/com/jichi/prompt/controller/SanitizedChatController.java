package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.config.InputSanitizer;
import com.jichi.prompt.constant.SecurityPrompts;
import com.jichi.prompt.entity.SanitizeResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/safe-ask")
public class SanitizedChatController {

    private final InputSanitizer inputSanitizer;
    private final ChatClient chatClient;

    public SanitizedChatController(InputSanitizer inputSanitizer,
                                    DashScopeChatModel chatModel) {
        this.inputSanitizer = inputSanitizer;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SecurityPrompts.SECURE_SYSTEM_PROMPT)
                .build();
    }

    record AskRequest(String message) {}

    @PostMapping
    public ResponseEntity<String> ask(@RequestBody AskRequest req) {
        // 第一道防线：规则过滤
        SanitizeResult check = inputSanitizer.sanitize(req.message());
        if (check.blocked()) {
            return ResponseEntity.badRequest()
                    .body("输入被拦截：" + check.message());
        }

        // 通过后才调用模型
        String reply = chatClient.prompt()
                .user(check.cleanedInput())
                .call()
                .content();

        return ResponseEntity.ok(reply);
    }
}