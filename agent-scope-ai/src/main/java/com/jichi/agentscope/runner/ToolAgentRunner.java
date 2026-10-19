package com.jichi.agentscope.runner;

import com.jichi.agentscope.hook.ToolMonitorHook;
import com.jichi.agentscope.tool.DateTimeTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

//@Component
public class ToolAgentRunner implements ApplicationRunner {

    private final DashScopeChatModel model;
    private final DateTimeTool dateTimeTool;

    public ToolAgentRunner(DashScopeChatModel model, DateTimeTool dateTimeTool) {
        this.model = model;
        this.dateTimeTool = dateTimeTool;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 创建 Toolkit 并注册工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(dateTimeTool);

        // 把 Toolkit 挂给 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("时间助手")
                .sysPrompt("你是一个助手，可以查询当前时间。")
                .model(model)
                .toolkit(toolkit)
                .hooks(List.of(new ToolMonitorHook()))  // 用 hooks() 传 List
                .build();

        Msg response = agent.call(
                Msg.builder().textContent("现在几点了？").build()
        ).block();

        System.out.println(response.getTextContent());

        if (response.getChatUsage() != null) {
            var usage = response.getChatUsage();
            System.out.printf("输入 Token：%d，输出 Token：%d%n",
                    usage.getInputTokens(), usage.getOutputTokens());
        }

        // 第一轮
        Msg r1 = agent.call(Msg.builder().textContent("我叫鸡哥").build()).block();
        System.out.println(r1.getTextContent());  // "你好，鸡哥！"

        // 第二轮——Agent 记得第一轮说了什么
        Msg r2 = agent.call(Msg.builder().textContent("我叫什么名字？").build()).block();
        System.out.println(r2.getTextContent());  // "你叫鸡哥。"

    }
}