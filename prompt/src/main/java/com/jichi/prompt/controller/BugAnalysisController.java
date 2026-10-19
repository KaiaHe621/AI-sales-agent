package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bug")
public class BugAnalysisController {

    private final ChatClient chatClient;

    public BugAnalysisController(DashScopeChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个资深 Java 工程师，擅长 bug 分析。
                        分析代码时，先推理出 bug 类型和根因，再填写结构化结论。
                        不确定的字段填 null。
                        """)
                .build();
    }

    record BugAnalysis(
            String bugType,
            String rootCause,
            List<String> affectedScenarios,
            String severity,
            String fix
    ) {
    }

    @PostMapping("/analyze")
    public BugAnalysis analyzeBug(@RequestBody String code) {
        return chatClient.prompt()
                .user("分析这段代码的 bug：\n\n" + code)
                .call()
                .entity(BugAnalysis.class);
    }
}