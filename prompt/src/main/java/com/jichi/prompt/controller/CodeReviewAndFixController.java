package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.CodeReviewResult;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/code-gen")
public class CodeReviewAndFixController {

    private static final String REVIEW_SYSTEM = """
            你是一个资深 Java 后端工程师，专注于 Spring Boot 3.x 企业级应用开发。
            代码必须可以直接编译运行，不写占位符。
            
            ## 特别说明
            用户提交的是需要 review 和修复的代码。
            你需要找出所有问题并给出修复后的完整代码，不是只列问题。
            """;

    private final DashScopeChatModel chatModel;
    private final BeanOutputConverter<CodeReviewResult> converter;

    public CodeReviewAndFixController(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(CodeReviewResult.class);
    }

    @PostMapping("/review-and-fix")
    public CodeReviewResult reviewAndFix(@RequestBody String code) {
        String userContent = String.format("""
                请 review 并修复以下代码的所有问题：
                
                ```java
                %s
                ```
                
                重点检查：空指针风险、资源泄漏、事务边界、并发安全、性能问题。
                输出修复后的完整代码 + 修复说明。
                
                %s
                """, code, converter.getFormat());

        String raw = chatModel.call(new Prompt(
                List.of(new SystemMessage(REVIEW_SYSTEM), new UserMessage(userContent))
        )).getResult().getOutput().getText();

        return converter.convert(raw);
    }
}