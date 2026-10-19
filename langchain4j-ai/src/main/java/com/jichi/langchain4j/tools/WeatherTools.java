package com.jichi.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class WeatherTools {

    @Tool("查询指定城市的实时天气信息，包括温度、天气状况和风力")
    public String getWeather(
            @P("城市名称，例如：北京、上海、广州") String city) {
        // 实际项目调用天气 API，这里用 Mock 数据演示
        return String.format("%s：晴天，温度18°C，风力2级", city);
    }

    @Tool("查询未来几天的天气预报")
    public String getWeatherForecast(
            @P("城市名称") String city,
            @P("查询天数，1-7之间的整数") int days) {
        return String.format("%s未来%d天：周一晴18°C，周二多云15°C……", city, days);
    }
}