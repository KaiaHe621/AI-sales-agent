package com.jichi.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class TimeoutAwareTools {

    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 带超时的外部 API 调用
     * 关键点：超时后返回字符串告知模型，不要抛异常——模型可以基于这个提示做后续决策
     */
    @Tool(description = "查询外部天气 API，获取实时天气数据")
    public String getWeatherFromApi(
            @ToolParam(description = "城市名") String city) {

        Future<String> future = executor.submit(() -> callExternalWeatherApi(city));

        try {
            return future.get(5, TimeUnit.SECONDS);  // 5 秒超时
        } catch (TimeoutException e) {
            future.cancel(true);
            return "天气服务响应超时（5秒），无法获取实时数据，请告知用户稍后重试";
        } catch (Exception e) {
            return "天气服务异常：" + e.getMessage() + "，请告知用户稍后重试";
        }
    }

    private String callExternalWeatherApi(String city) {
        // 调用外部 API...
        return "";
    }
}