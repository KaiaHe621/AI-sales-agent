package com.jichi.springai.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ContentSafetyAdvisor implements CallAdvisor {

    // 实际项目可以对接阿里云内容安全、腾讯云天御等服务
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "违禁词1", "违禁词2"
    );

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. 检查输入
        String userText = request.prompt().getContents();
        if (containsBlockedContent(userText)) {
            // 拦截，返回一个"安全"的响应，不真正调用模型
            return buildSafeResponse(request, "您的输入包含不当内容，请重新输入。");
        }

        // 2. 正常调用
        ChatClientResponse response = chain.nextCall(request);

        // 3. 检查输出
        String aiContent = response.chatResponse().getResult().getOutput().getText();
        if (containsBlockedContent(aiContent)) {
            // 模型输出了不当内容，替换
            return buildSafeResponse(request, "内容审核未通过，请换个问题试试。");
        }

        return response;
    }

    private boolean containsBlockedContent(String text) {
        if (text == null) return false;
        return BLOCKED_KEYWORDS.stream().anyMatch(text::contains);
    }

    private ChatClientResponse buildSafeResponse(ChatClientRequest request, String message) {
        // 构造一个假的 ChatResponse 返回
        AssistantMessage assistantMessage = new AssistantMessage(message);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        return ChatClientResponse.builder()
                .chatResponse(chatResponse)
                .context(request.context())
                .build();
    }

    @Override
    public String getName() {
        return "ContentSafetyAdvisor";
    }

    @Override
    public int getOrder() {
        return 5; // 在限流之后执行
    }
}