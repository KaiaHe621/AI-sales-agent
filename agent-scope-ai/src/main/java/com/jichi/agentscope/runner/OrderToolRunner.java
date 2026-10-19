package com.jichi.agentscope.runner;

import com.jichi.agentscope.tool.OrderTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class OrderToolRunner implements ApplicationRunner {

    private final DashScopeChatModel model;
    private final OrderTool orderTool;

    public OrderToolRunner(DashScopeChatModel model, OrderTool orderTool) {
        this.model = model;
        this.orderTool = orderTool;
    }

    @Override
    public void run(ApplicationArguments args) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(orderTool);

        ReActAgent agent = ReActAgent.builder()
                .name("订单助手")
                .model(model)
                .sysPrompt("你可以帮用户创建订单，下单后告诉用户订单号。")
                .toolkit(toolkit)
                .build();

        Msg response = agent.call(
                Msg.builder().textContent(
                        "帮我买 2 件商品 SKU-888，收货地址是北京市朝阳区 XX 街道 10 号"
                ).build()
        ).block();

        System.out.println("[下单结果] " + response.getTextContent());
    }
}