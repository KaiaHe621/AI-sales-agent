package com.jichi.springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class JavaTechService {

    private final ChatClient chatClient;

    public JavaTechService(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("""
                        你是一个专业的 Java 技术助手。
                        
                        职责：
                        - 回答 Java、Spring Boot、Spring AI 相关的技术问题
                        - 帮助用户 debug 代码
                        - 提供最佳实践建议
                        
                        规则：
                        - 代码示例使用 Java 17+ 语法
                        - 回答简洁，不要过度解释
                        - 不确定的内容要说明，不要编造
                        - 非技术问题礼貌拒绝
                        
                        输出格式：
                        - 使用 Markdown 格式
                        - 代码用代码块包裹
                        """)
                .build();
    }

    public String ask(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}