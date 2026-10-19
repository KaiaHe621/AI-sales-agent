package com.jichi.agentscope.runner;

import com.jichi.agentscope.tool.AsyncSearchTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class AsyncToolRunner implements ApplicationRunner {

    private final DashScopeChatModel model;
    private final AsyncSearchTool asyncSearchTool;

    public AsyncToolRunner(DashScopeChatModel model, AsyncSearchTool asyncSearchTool) {
        this.model = model;
        this.asyncSearchTool = asyncSearchTool;
    }

    @Override
    public void run(ApplicationArguments args) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(asyncSearchTool);

        ReActAgent agent = ReActAgent.builder()
                .name("搜索助手")
                .model(model)
                .sysPrompt("你可以搜索互联网上的最新信息。")
                .toolkit(toolkit)
                .build();

        Msg response = agent.call(
                Msg.builder().textContent("帮我搜索一下 AgentScope Java 最新版本的特性").build()
        ).block();

        System.out.println("[搜索结果] " + response.getTextContent());
    }
}