package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.config.InputSanitizer;
import com.jichi.prompt.constant.SecurityPrompts;
import com.jichi.prompt.entity.SanitizeResult;
import com.jichi.prompt.service.IntentGuard;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/secure-chat")
public class SecureChatController {

    private final InputSanitizer inputSanitizer;
    private final IntentGuard intentGuard;
    private final ChatClient chatClient;

    public SecureChatController(InputSanitizer inputSanitizer,
                                IntentGuard intentGuard,
                                DashScopeChatModel chatModel) {
        this.inputSanitizer = inputSanitizer;
        this.intentGuard = intentGuard;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SecurityPrompts.SECURE_SYSTEM_PROMPT)
                .build();
    }

    record AskRequest(String message) {
    }

    @PostMapping("/ask")
    public ResponseEntity<String> ask(@RequestBody AskRequest req) {
        // 第一道：规则过滤（关键词 + 正则，无额外 API 调用）
        SanitizeResult sanitize = inputSanitizer.sanitize(req.message());
        if (sanitize.blocked()) {
            return ResponseEntity.badRequest()
                    .body("输入被拦截：" + sanitize.message());
        }

        // 第二道：AI 意图检测（能识别复杂、变形的注入）
        if (!intentGuard.isSafe(req.message())) {
            return ResponseEntity.badRequest()
                    .body("输入包含不当内容，请重新输入");
        }

        // 两道都过了，才调用模型
        String reply = chatClient.prompt()
                .user(req.message())
                .call()
                .content();

        return ResponseEntity.ok(reply);
    }
}