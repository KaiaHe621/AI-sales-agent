package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/sentiment-messages")
public class SentimentMessagesController {

    private final ChatClient chatClient;

    public SentimentMessagesController(DashScopeChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    @GetMapping
    public String analyzeWithMessages(@RequestParam String comment) {
        List<Message> messages = new ArrayList<>();

        messages.add(new SystemMessage("对用户评论进行情感分析，输出 POSITIVE/NEGATIVE/NEUTRAL。"));

        // 示例对话（模拟历史对话格式）
        messages.add(new UserMessage("物流很快，东西也不错，就是包装有点简单"));
        messages.add(new AssistantMessage("POSITIVE"));

        messages.add(new UserMessage("快递慢，客服态度差，商品也有破损"));
        messages.add(new AssistantMessage("NEGATIVE"));

        messages.add(new UserMessage("和描述一致，正常收到，没什么特别的"));
        messages.add(new AssistantMessage("NEUTRAL"));

        messages.add(new UserMessage(comment));

        return chatClient.prompt()
                .messages(messages)
                .call()
                .content()
                .trim();
    }
}