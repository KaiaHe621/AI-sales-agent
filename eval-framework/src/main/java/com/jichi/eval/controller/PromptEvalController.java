package com.jichi.eval.controller;

import com.jichi.eval.prompt.PromptEvalService;
import com.jichi.eval.prompt.PromptRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eval/prompt")
@RequiredArgsConstructor
public class PromptEvalController {

    private final PromptEvalService promptEvalService;
    private final PromptRegistry promptRegistry;

    /**
     * 注册两个版本的 Prompt（实际项目里从数据库或配置文件加载）
     */
    @PostConstruct
    public void registerPrompts() {
        promptRegistry.register("v1.0", """
                你是一名专业的技术问答助手。
                请用中文回答用户的技术问题，要求简洁、准确、不超过200字。
                """, "原始版本");

        promptRegistry.register("v1.1-detailed", """
                你是一名资深技术专家，精通 Java、Spring Boot 和 AI 开发。
                请用中文回答用户的技术问题。要求：
                1. 先给出一句话核心答案
                2. 再展开解释原理（如有必要）
                3. 如有实践建议，请列出
                回答控制在300字以内。
                """, "增加结构化要求，鼓励展开原理");
    }

    /**
     * 列出所有注册的 Prompt 版本
     */
    @GetMapping("/versions")
    public Object listVersions() {
        return promptRegistry.listAll();
    }

    /**
     * 对两个 Prompt 版本做 A/B 对比
     */
    @PostMapping("/ab-test")
    public PromptEvalService.PromptABReport abTest(
            @RequestParam String versionA,
            @RequestParam String versionB,
            @RequestParam(defaultValue = "keywordEvaluator") String evaluator
    ) throws Exception {
        return promptEvalService.compare(versionA, versionB, evaluator);
    }
}