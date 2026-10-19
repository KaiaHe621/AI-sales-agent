package com.jichi.langchain4j.tools.agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class CityWeatherTools {
    @Tool("查询城市实时天气，返回天气状况和温度")
    public String getWeather(@P("城市名称") String city) {
        return city + "：晴天，18°C，风力2级";
    }
}