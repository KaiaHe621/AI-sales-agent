package com.jichi.springai.controller.chatMemory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/manual-chat")
public class ManualChatController {

    private final ChatClient chatClient;
    // 手动维护每个会话的历史（演示用，生产不推荐）
    private final Map<String, List<Message>> sessions = new ConcurrentHashMap<>();

    public ManualChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping
    public String chat(@RequestBody ChatRequest request) {
        // 获取或创建该会话的历史
        List<Message> history = sessions.computeIfAbsent(request.conversationId(), id -> {
            List<Message> list = new ArrayList<>();
            list.add(new SystemMessage("你是一个 Java 技术助手"));
            return list;
        });

        // 追加用户消息
        history.add(new UserMessage(request.message()));

        // 带完整历史调用模型
        String reply = chatClient.prompt()
                .messages(history)
                .call()
                .content();

        // 把模型回复也追加进历史
        history.add(new AssistantMessage(reply));

        return reply;
    }

    record ChatRequest(String conversationId, String message) {
    }
}