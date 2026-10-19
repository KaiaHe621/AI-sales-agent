package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.PromptEvaluation;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/meta-prompt-eval")
public class MetaPromptEvaluationController {

    private static final String EVALUATION_META_PROMPT = """
            你是一个 Prompt 质量评估专家。
            对给定的 Prompt 从以下维度打分（1-10）并给出改进建议：
            
            1. 清晰度：任务目标是否清晰无歧义
            2. 完整性：是否涵盖必要的角色、任务、约束、格式
            3. 安全性：是否有足够的约束防止越界行为
            4. 简洁性：是否有冗余内容
            5. 可操作性：模型是否能明确知道该怎么做
            """;

    private final DashScopeChatModel chatModel;
    private final BeanOutputConverter<PromptEvaluation> converter;

    public MetaPromptEvaluationController(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(PromptEvaluation.class);
    }

    @PostMapping("/evaluate")
    public PromptEvaluation evaluatePrompt(@RequestBody String prompt) {
        String userContent = "请评估以下 Prompt：\n\n" + prompt + "\n\n" + converter.getFormat();

        String raw = chatModel.call(new Prompt(
                List.of(
                        new SystemMessage(EVALUATION_META_PROMPT),
                        new UserMessage(userContent)
                )
        )).getResult().getOutput().getText();

        return converter.convert(raw);
    }
}