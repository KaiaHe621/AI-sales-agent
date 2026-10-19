package com.jichi.springai.controller.chatClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class ChatCompareDemo {

    private final ChatClient chatClient;
    private final ChatModel chatModel;

    public ChatCompareDemo(ChatClient.Builder builder, ChatModel chatModel) {
        this.chatClient = builder.build();
        this.chatModel = chatModel;
    }

    public void compare() {
        // ChatModel 的原始用法——繁琐
        Prompt prompt = new Prompt(new UserMessage("你好"));
        ChatResponse response = chatModel.call(prompt);
        String content1 = response.getResult().getOutput().getText();

        // ChatClient 的用法——简洁
        String content2 = chatClient.prompt()
                .user("你好")
                .call()
                .content();
    }
}