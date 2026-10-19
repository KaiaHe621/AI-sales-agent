package com.jichi.springai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTools {

    /**
     * @Tool 注解把这个方法标记为 AI 可调用的工具
     * description 是给模型看的，告诉模型这个工具做什么用，写清楚一点
     */
    @Tool(description = "获取指定城市的当前天气。返回温度、天气状况、风力等信息。")
    public String getWeather(
            @ToolParam(description = "城市名称，例如：北京、上海、广州") String city) {

        // 这里调用真实的天气 API，演示用假数据
        return String.format("""
                城市：%s
                温度：25°C
                天气：晴
                风力：北风3级
                湿度：45%%
                更新时间：%s
                """, city, java.time.LocalDateTime.now());
    }

    @Tool(description = "获取未来几天的天气预报")
    public String getWeatherForecast(
            @ToolParam(description = "城市名称") String city,
            @ToolParam(description = "预报天数，1-7天") int days) {

        // 模拟天气预报数据
        StringBuilder sb = new StringBuilder();
        sb.append(city).append(" 未来 ").append(days).append(" 天天气预报：\n");
        String[] weathers = {"晴", "多云", "小雨", "阴", "大风"};
        for (int i = 1; i <= days; i++) {
            sb.append(String.format("第%d天：%s，20-%d°C\n", i, weathers[i % weathers.length], 20 + i));
        }
        return sb.toString();
    }
}