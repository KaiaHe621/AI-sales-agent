package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SentimentService {

    private final ChatClient chatClient;

    private static final String FEW_SHOT_TEMPLATE = """
            对用户评论进行情感分析，输出 POSITIVE、NEGATIVE 或 NEUTRAL。
            
            规则：提到优点多于缺点→POSITIVE；缺点多于优点→NEGATIVE；均等→NEUTRAL
            
            示例1：
            评论：物流很快，东西也不错，就是包装有点简单
            标签：POSITIVE
            
            示例2：
            评论：快递慢，客服态度差，商品也有破损
            标签：NEGATIVE
            
            示例3：
            评论：和描述一致，正常收到，没什么特别的
            标签：NEUTRAL
            
            现在请标注：
            评论：{comment}
            标签：
            """;

    public SentimentService(DashScopeChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel).build();
    }

    public String analyze(String comment) {
        return chatClient.prompt()
                .user(u -> u.text(FEW_SHOT_TEMPLATE).param("comment", comment))
                .call()
                .content()
                .trim();
    }

    public Map<String, String> analyzeBatch(List<String> comments) {
        Map<String, String> results = new LinkedHashMap<>();
        for (String comment : comments) {
            results.put(comment, analyze(comment));
        }
        return results;
    }
}