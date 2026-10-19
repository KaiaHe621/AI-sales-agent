package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.tools.CalculatorTools;
import com.jichi.prompt.tools.StockTools;
import com.jichi.prompt.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final ChatClient agentClient;

    public AgentController(
            DashScopeChatModel chatModel,
            WeatherTools weatherTools,
            StockTools stockTools,
            CalculatorTools calculatorTools) {

        this.agentClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个智能助手，可以查天气、查股价、做计算。
                        根据用户问题决定是否需要使用工具，使用工具后结合结果给出准确答案。
                        """)
                .defaultTools(weatherTools, stockTools, calculatorTools)
                .build();
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return agentClient.prompt()
                .user(question)
                .call()
                .content();
    }

    /**
     * 带循环次数限制的 Agent 调用（防止无限循环）
     */
    @GetMapping("/ask-safe")
    public String askSafe(@RequestParam String question) {
        return agentClient.prompt()
                .user(question)
                .call()
                .content();
    }
}