package com.jichi.agentscope.service;

import com.jichi.agentscope.tool.FlightSearchTool;
import com.jichi.agentscope.tool.OrderTool;
import com.jichi.agentscope.tool.WeatherTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.stereotype.Service;

@Service
public class TravelPlanService {

    private final DashScopeChatModel model;
    private final WeatherTool weatherTool;
    private final FlightSearchTool flightTool;
    private final OrderTool orderTool;

    public TravelPlanService(DashScopeChatModel model,
                             WeatherTool weatherTool,
                             FlightSearchTool flightTool,
                             OrderTool orderTool) {
        this.model = model;
        this.weatherTool = weatherTool;
        this.flightTool = flightTool;
        this.orderTool = orderTool;
    }

    public String plan(String departure, String destination, String date) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(weatherTool);
        toolkit.registerTool(flightTool);
        toolkit.registerTool(orderTool);

        ReActAgent agent = ReActAgent.builder()
                .name("出行助手")
                .model(model)
                .sysPrompt("""
                        你是一个出行规划助手。
                        处理出行规划任务时，按以下步骤执行：
                        1. 查询目的地天气，评估出行条件
                        2. 搜索出发地到目的地的可用航班
                        3. 查询用户现有订单，避免时间冲突
                        4. 综合以上信息给出出行建议
                        每个步骤完成后更新子任务状态。
                        """)
                .toolkit(toolkit)
                .enablePlan()
                .build();

        Msg response = agent.call(
                Msg.builder()
                        .textContent(String.format(
                                "帮我规划 %s 从 %s 出发去 %s 的行程", date, departure, destination))
                        .build()
        ).block();

        return response.getTextContent();
    }
}