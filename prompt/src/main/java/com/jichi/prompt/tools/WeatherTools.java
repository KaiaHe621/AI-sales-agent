package com.jichi.prompt.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTools {

    @Tool(description = "查询指定城市的实时天气，返回温度、天气状况和风力")
    public String getWeather(
            @ToolParam(description = "城市名称，例如：北京、上海") String city) {
        return String.format("城市：%s，温度：18°C，天气：晴，风力：3级", city);
    }
}


