package com.jichi.springai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PromptDemoService {

    private final ChatClient chatClient;

    public PromptDemoService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 代码 Review
     */
    public String codeReview(String language, String code) {
        return chatClient.prompt()
                .system(s -> s.text("""
                                你是一个资深 {language} 工程师，做 code review。
                                找出 Bug、性能问题、代码风格问题，每个问题注明严重程度（高/中/低）。
                                """)
                        .param("language", language))
                .user(u -> u.text("请 review 这段代码：\n```\n{code}\n```")
                        .param("code", code))
                .call()
                .content();
    }

    /**
     * 文档翻译
     */
    public String translate(String text, String targetLanguage) {
        PromptTemplate template = new PromptTemplate("""
                将下面的文字翻译成 {targetLanguage}，保持原文语气，不要意译：
                
                {text}
                """);

        Prompt prompt = template.create(Map.of(
                "targetLanguage", targetLanguage,
                "text", text
        ));

        return chatClient.prompt(prompt)
                .call()
                .content();
    }

    /**
     * 限定领域问答
     */
    public String domainQA(String domain, String question) {
        return chatClient.prompt()
                .system(s -> s.text("""
                                你是一个 {domain} 领域的专家顾问。
                                只回答和 {domain} 相关的问题，其他问题拒绝回答。
                                不确定的内容要明确说明，不要编造。
                                """)
                        .param("domain", domain))
                .user(question)
                .call()
                .content();
    }
}