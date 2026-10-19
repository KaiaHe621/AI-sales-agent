package com.jichi.langchain4j.service.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class TimeoutWeatherTools {

    private final RestTemplate restTemplate;

    private static final Map<String, String> MOCK_WEATHER = Map.of(
            "北京", "晴天 18°C，湿度 30%，东风3级",
            "上海", "多云 22°C，湿度 65%，东南风2级",
            "广州", "小雨 26°C，湿度 80%，南风2级"
    );

    public TimeoutWeatherTools() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);   // 连接超时 3s
        factory.setReadTimeout(5000);      // 读超时 5s
        this.restTemplate = new RestTemplate(factory);
    }

    @Tool("查询实时天气")
    public String getWeather(@P("城市名称") String city) {
        try {
            // Mock 数据；生产中替换为 restTemplate.getForEntity(...) 调用真实 API
            String weather = MOCK_WEATHER.getOrDefault(city, "晴天 20°C，湿度 45%，微风");
            return city + "当前天气：" + weather;
        } catch (ResourceAccessException e) {
            log.warn("天气查询超时，城市：{}", city);
            return "天气服务暂时不可用，请稍后再试";
        }
    }
}