package com.jichi.agentscope.runner;

import com.jichi.agentscope.tool.WeatherTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class WeatherToolRunner implements ApplicationRunner {

    private final DashScopeChatModel model;
    private final WeatherTool weatherTool;

    public WeatherToolRunner(DashScopeChatModel model, WeatherTool weatherTool) {
        this.model = model;
        this.weatherTool = weatherTool;
    }

    @Override
    public void run(ApplicationArguments args) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(weatherTool);

        ReActAgent agent = ReActAgent.builder()
                .name("天气助手")
                .model(model)
                .sysPrompt("你可以查询城市天气，请直接给出结果。")
                .toolkit(toolkit)
                .build();

        Msg response = agent.call(
                Msg.builder().textContent("北京今天天气怎么样？").build()
        ).block();

        System.out.println("[天气结果] " + response.getTextContent());
    }
}