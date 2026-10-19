package com.jichi.springaialibaba.controller.multi;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class DynamicModelController {

    private final Map<String, ChatModel> chatModels;

    public DynamicModelController(OpenAiChatModel openAiChatModel,
                                  DashScopeChatModel dashScopeChatModel) {
        this.chatModels = Map.of(
                "deepseek", openAiChatModel,
                "qwen", dashScopeChatModel
        );
    }

    /**
     * 用户可以通过 provider 参数选择模型
     */
    @GetMapping
    public String chat(
            @RequestParam String message,
            @RequestParam(defaultValue = "deepseek") String provider) {

        ChatModel model = chatModels.getOrDefault(provider, chatModels.get("deepseek"));

        return ChatClient.builder(model)
                .build()
                .prompt()
                .user(message)
                .call()
                .content();
    }
}