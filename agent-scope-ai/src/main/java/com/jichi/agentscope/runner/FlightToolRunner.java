package com.jichi.agentscope.runner;

import com.jichi.agentscope.tool.FlightSearchTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class FlightToolRunner implements ApplicationRunner {

    private final DashScopeChatModel model;
    private final FlightSearchTool flightSearchTool;

    public FlightToolRunner(DashScopeChatModel model, FlightSearchTool flightSearchTool) {
        this.model = model;
        this.flightSearchTool = flightSearchTool;
    }

    @Override
    public void run(ApplicationArguments args) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(flightSearchTool);

        ReActAgent agent = ReActAgent.builder()
                .name("航班助手")
                .model(model)
                .sysPrompt("你可以帮用户查询航班信息。")
                .toolkit(toolkit)
                .build();

        // 不传 cabin，LLM 应默认使用经济舱
        Msg r1 = agent.call(
                Msg.builder().textContent("帮我查 2025-06-01 北京飞广州的航班").build()
        ).block();
        System.out.println("[经济舱] " + r1.getTextContent());

        // 传 cabin，LLM 应识别并传 business
        Msg r2 = ReActAgent.builder()
                .name("航班助手")
                .model(model)
                .sysPrompt("你可以帮用户查询航班信息。")
                .toolkit(toolkit)
                .build()
                .call(Msg.builder().textContent("查一下 2025-06-01 上海到成都的商务舱").build())
                .block();
        System.out.println("[商务舱] " + r2.getTextContent());
    }
}