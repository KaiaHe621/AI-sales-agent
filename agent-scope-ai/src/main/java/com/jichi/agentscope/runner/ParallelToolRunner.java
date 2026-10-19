package com.jichi.agentscope.runner;

import com.jichi.agentscope.tool.WeatherTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class ParallelToolRunner implements ApplicationRunner {

    private final DashScopeChatModel model;
    private final WeatherTool weatherTool;

    public ParallelToolRunner(DashScopeChatModel model, WeatherTool weatherTool) {
        this.model = model;
        this.weatherTool = weatherTool;
    }

    @Override
    public void run(ApplicationArguments args) {
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder().parallel(true).build());
        toolkit.registerTool(weatherTool);

        ReActAgent agent = ReActAgent.builder()
                .name("多城市天气助手")
                .model(model)
                .sysPrompt("你可以查询多个城市的天气，请同时查询所有城市并汇总。")
                .toolkit(toolkit)
                .build();

        long start = System.currentTimeMillis();
        Msg response = agent.call(
                Msg.builder().textContent("请同时查询北京、上海、广州三个城市的天气").build()
        ).block();
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("[并行结果] " + response.getTextContent());
        System.out.printf("[耗时] %d ms（3 个工具并行，耗时约等于单次调用时间）%n", elapsed);
    }
}