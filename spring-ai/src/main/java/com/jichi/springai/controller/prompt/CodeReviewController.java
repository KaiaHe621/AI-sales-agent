package com.jichi.springai.controller.prompt;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CodeReviewController {

    private final ChatClient chatClient;

    public CodeReviewController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/review")
    public String codeReview(@RequestParam String code) {
        return chatClient.prompt()
                .system("""
                        你是一个资深 Java 工程师，正在做 code review。
                        找出代码中的：
                        1. Bug（包括潜在的运行时错误）
                        2. 性能问题
                        3. 不符合 Java 最佳实践的写法
                        
                        输出格式：
                        每个问题单独列出，格式为：
                        【问题类型】问题描述
                        原代码：...
                        建议修改：...
                        """)
                .user("请 review 这段代码：\n" + code)
                .call()
                .content();
    }

}