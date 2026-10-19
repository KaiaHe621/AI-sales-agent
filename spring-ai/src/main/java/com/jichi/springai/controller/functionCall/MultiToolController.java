package com.jichi.springai.controller.functionCall;

import com.jichi.springai.tools.WeatherTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

/**
 * 演示多工具 + 多次调用：
 * 同时注册天气查询和天气预报两个工具，
 * 用户一句话可能触发多次工具调用
 */
@RestController
@RequestMapping("/api/multi-tool")
public class MultiToolController {

    private final ChatClient chatClient;
    private final WeatherTools weatherTools;

    public MultiToolController(ChatClient.Builder builder, WeatherTools weatherTools) {
        this.weatherTools = weatherTools;
        this.chatClient = builder
                .defaultSystem("你是一个天气助手，可以查询实时天气和天气预报。不要编造数据。")
                .build();
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .tools(weatherTools)   // 注册了 getWeather 和 getWeatherForecast 两个工具
                .call()
                .content();
    }
}