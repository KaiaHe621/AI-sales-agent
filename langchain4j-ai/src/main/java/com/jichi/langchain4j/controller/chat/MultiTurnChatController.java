package com.jichi.langchain4j.controller.chat;

import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class MultiTurnChatController {

    private final ChatModel chatModel;

    public MultiTurnChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/multi-turn")
    public String multiTurnChat(@RequestBody MultiTurnRequest request) {
        List<ChatMessage> messages = new ArrayList<>();

        // 系统消息（角色设定）
        messages.add(new SystemMessage("你是一个 Java 技术助手，只回答 Java 相关问题。"));

        // 历史对话
        for (HistoryMessage h : request.history()) {
            messages.add(new UserMessage(h.user()));
            messages.add(new AiMessage(h.assistant()));
        }

        // 当前用户消息
        messages.add(new UserMessage(request.message()));

        AiMessage response = chatModel.chat(messages).aiMessage();
        return response.text();
    }

    record MultiTurnRequest(
            List<HistoryMessage> history,
            String message
    ) {
    }

    record HistoryMessage(String user, String assistant) {
    }
}