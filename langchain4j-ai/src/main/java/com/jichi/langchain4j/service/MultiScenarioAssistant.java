package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface MultiScenarioAssistant {

    // 场景一：简单问答
    @SystemMessage("你是一个 Java 技术助手，回答简洁")
    String techChat(String question);

    // 场景二：有格式要求的翻译
    @SystemMessage("你是专业翻译，保持原文风格，不添加解释")
    @UserMessage("将以下内容翻译成{{language}}：\n\n{{content}}")
    String translate(@V("language") String lang, @V("content") String content);

    // 场景三：从文件加载复杂 Prompt
    @SystemMessage(fromResource = "prompts/code-review.txt")
    String reviewCode(@V("language") String lang, @UserMessage String code);

    // 场景四：多变量 + 对象参数
    @SystemMessage("你是数据分析专家")
    @UserMessage("分析以下{{period}}的销售数据，重点关注{{focus}}：\n{{data}}")
    String analyzeData(@V("period") String period,
                       @V("focus") String focus,
                       @V("data") String data);
}