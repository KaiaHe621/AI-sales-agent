package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/thinking")
public class ThinkingController {

    private final DashScopeChatModel chatModel;

    public ThinkingController(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/qwen3")
    public String deepAnalysis(@RequestParam String question) {
        return chatModel.call(new Prompt(
                new UserMessage(question),
                DashScopeChatOptions.builder()
                        .withModel("qwen3-235b-a22b")  // Qwen3 支持思考模式的模型
                        .withEnableThinking(true)       // 开启内置思考模式
                        .build()
        )).getResult().getOutput().getText();
    }
}