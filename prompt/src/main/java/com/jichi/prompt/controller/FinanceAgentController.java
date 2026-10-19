package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.tools.FinanceTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/finance-agent")
public class FinanceAgentController {

    private final ChatClient agentClient;

    public FinanceAgentController(DashScopeChatModel chatModel, FinanceTools financeTools) {
        this.agentClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个金融助手，可以查询实时股价、汇率，并做数学计算。
                        遇到需要数据的问题，先调工具获取数据，再给出完整答案。
                        """)
                .defaultTools(financeTools)
                .build();
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return agentClient.prompt()
                .user(question)
                .call()
                .content();
    }
}