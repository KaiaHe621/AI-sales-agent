package com.jichi.springai.controller.functionCall;

import com.jichi.springai.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
public class WeatherChatController {

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;

    public WeatherChatController(ChatClient.Builder builder, WeatherTools weatherTools) {
        this.weatherTools = weatherTools;
        this.chatClient = builder
                .defaultSystem("你是一个天气助手，帮用户查询天气信息。不要编造天气数据，只根据工具返回的信息回答。")
                .build();
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                // 注册工具，可以传多个
                .tools(weatherTools)
                .call()
                .content();
    }
}