package com.jichi.agentscope.runner;

import com.jichi.agentscope.hook.LoggingHook;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

//@Component
public class HelloWorldRunner implements ApplicationRunner {

    private final DashScopeChatModel model;

    public HelloWorldRunner(DashScopeChatModel model) {
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("助手")
                .sysPrompt("你是一个友好的助手，回答简洁清晰。")
                .hooks(List.of(new LoggingHook()))
                .model(model)
                .build();

        // 构造消息并发送
        Msg userMsg = Msg.builder()
                .textContent("你好，介绍一下你自己")
                .build();

        // call() 返回 Mono<Msg>，调用 block() 等待结果
        Msg response = agent.call(userMsg).block();

        System.out.println("Agent 回复：" + response.getTextContent());
    }
}