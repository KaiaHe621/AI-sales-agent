package com.jichi.agentscope.runner;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class InMemoryMemoryRunner implements ApplicationRunner {

    private final DashScopeChatModel model;

    public InMemoryMemoryRunner(DashScopeChatModel model) {
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 不指定 .memory() 时默认用 InMemoryMemory，消息历史存在 JVM 内存里
        ReActAgent agent = ReActAgent.builder()
                .name("助手")
                .model(model)
                .sysPrompt("你是一个助手。")
                .build();

        // 第一轮：告诉 Agent 项目背景
        agent.call(Msg.builder().textContent("我在做一个电商项目").build()).block();
        System.out.println("[第一轮] 已告知：我在做一个电商项目");

        // 第二轮：补充需求
        agent.call(Msg.builder().textContent("项目需要用到库存管理功能").build()).block();
        System.out.println("[第二轮] 已告知：需要库存管理功能");

        // 第三轮：测试记忆，看 Agent 是否还记得
        Msg r = agent.call(
                Msg.builder().textContent("刚才我说的项目是什么，需要哪些功能").build()
        ).block();
        System.out.println("[第三轮 Agent 回答] " + r.getTextContent());
        // 预期：Agent 能正确说出电商项目 + 库存管理功能
    }
}