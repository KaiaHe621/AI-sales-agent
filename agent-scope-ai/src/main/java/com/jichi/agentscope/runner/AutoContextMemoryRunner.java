package com.jichi.agentscope.runner;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.memory.autocontext.ContextOffloadTool;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class AutoContextMemoryRunner implements ApplicationRunner {

    private final DashScopeChatModel model;

    public AutoContextMemoryRunner(DashScopeChatModel model) {
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 配置压缩策略：消息数超过 10 条时触发（演示用，生产建议 30-50）
        AutoContextConfig config = AutoContextConfig.builder()
                .msgThreshold(5)     // 消息数触发阈值
                .lastKeep(5)          // 压缩时保留最近 5 条消息不动
                .tokenRatio(0.3)      // Token 使用率超过 30% 时触发
                .build();

        // AutoContextMemory 需要传入 model，触发压缩时用它生成摘要
        AutoContextMemory memory = new AutoContextMemory(config, model);

        // ContextOffloadTool：让 Agent 能按需重载被卸载的历史内容
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new ContextOffloadTool(memory));

        ReActAgent agent = ReActAgent.builder()
                .name("助手")
                .model(model)
                .sysPrompt("你是一个助手，能记住对话历史。")
                .memory(memory)
                .toolkit(toolkit)
                .build();

        // 模拟多轮对话，观察消息数变化
        String[] questions = {
                "我在做一个电商项目",
                "项目技术栈是 Spring Boot + MySQL",
                "我遇到了库存超卖的问题",
                "我打算用分布式锁解决",
                "刚才我说的项目背景是什么"   // 测试记忆是否生效
        };

        for (String q : questions) {
            Msg response = agent.call(Msg.builder().textContent(q).build()).block();
            System.out.printf("[消息数: %d] 用户: %s%n", memory.getMessages().size(), q);
            System.out.println("[Agent] " + response.getTextContent());
            System.out.println("---");
        }
    }
}