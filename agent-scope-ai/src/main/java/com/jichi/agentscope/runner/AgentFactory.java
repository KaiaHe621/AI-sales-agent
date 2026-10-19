package com.jichi.agentscope.runner;

import com.jichi.agentscope.tool.WeatherTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.stereotype.Component;

@Component
public class AgentFactory {

    private final DashScopeChatModel model;
    private final WeatherTool weatherTool;

    public AgentFactory(DashScopeChatModel model, WeatherTool weatherTool) {
        this.model = model;
        this.weatherTool = weatherTool;
    }

    public ReActAgent createWeatherAgent() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(weatherTool);

        return ReActAgent.builder()
                .name("天气助手")
                .model(model)
                .sysPrompt("你可以查询城市天气。")
                .toolkit(toolkit)
                .build();
    }
}