package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/meta-prompt")
public class MetaPromptController {

    private static final String META_PROMPT = """
            你是一个 Prompt 工程师，专门为 AI 应用设计高质量的 System Prompt。
            
            用户会描述一个业务场景，你需要生成一个完整的 System Prompt，要求：
            1. 包含明确的角色定位
            2. 包含清晰的任务边界
            3. 包含必要的行为约束
            4. 包含输出格式要求（如果有需要）
            5. 语言简洁，避免冗余
            6. 针对业务场景加入具体的示例（如果有助于理解）
            
            直接输出生成的 System Prompt，不要有任何解释或前缀。
            """;

    private static final String OPTIMIZE_META_PROMPT = """
            你是一个 Prompt 优化专家。用户会提供：
            1. 当前 Prompt（当前版本）
            2. 期望的任务和输出
            3. 现在存在的问题
            
            你需要：
            1. 分析当前 Prompt 存在的问题（逐条说明）
            2. 给出优化后的 Prompt
            3. 解释每处改动的原因
            """;

    public record PromptOptimizationRequest(
            String currentPrompt,
            String expectedTask,
            String issues
    ) {
    }

    public record PromptOptimizationResult(
            java.util.List<String> issues,
            String optimizedPrompt,
            java.util.List<String> changes
    ) {
    }

    private final DashScopeChatModel chatModel;
    private final BeanOutputConverter<PromptOptimizationResult> converter;

    public MetaPromptController(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(PromptOptimizationResult.class);
    }

    @GetMapping("/generate")
    public String generatePrompt(@RequestParam String scenario) {
        return chatModel.call(new Prompt(
                java.util.List.of(
                        new org.springframework.ai.chat.messages.SystemMessage(META_PROMPT),
                        new org.springframework.ai.chat.messages.UserMessage(
                                "请为以下场景生成 System Prompt：\n\n" + scenario)
                )
        )).getResult().getOutput().getText();
    }

    @PostMapping("/optimize")
    public PromptOptimizationResult optimizePrompt(@RequestBody PromptOptimizationRequest req) {
        String userContent = String.format("""
                        当前 Prompt：
                        %s
                        
                        期望的任务：
                        %s
                        
                        存在的问题：
                        %s
                        
                        %s
                        """, req.currentPrompt(), req.expectedTask(), req.issues(),
                converter.getFormat());

        String raw = chatModel.call(new Prompt(
                java.util.List.of(
                        new org.springframework.ai.chat.messages.SystemMessage(OPTIMIZE_META_PROMPT),
                        new org.springframework.ai.chat.messages.UserMessage(userContent)
                )
        )).getResult().getOutput().getText();

        return converter.convert(raw);
    }
}