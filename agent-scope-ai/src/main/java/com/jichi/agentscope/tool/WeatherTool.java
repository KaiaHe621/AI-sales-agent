package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTool {

    @Tool(name = "get_weather", description = "查询指定城市的实时天气")
    public String getWeather(
            @ToolParam(name = "city", description = "城市名称，如：北京、上海") String city
    ) {
        return String.format("%s 今日天气：晴，气温 18°C，湿度 45%%", city);
    }
}