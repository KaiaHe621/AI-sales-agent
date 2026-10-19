package com.jichi.agentscope.runner;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.DashScopeChatModel;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.List;

//@Component
public class MemoryOperationRunner implements ApplicationRunner {

    private final DashScopeChatModel model;

    public MemoryOperationRunner(DashScopeChatModel model) {
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) {
        ReActAgent agent = ReActAgent.builder()
                .name("助手")
                .model(model)
                .sysPrompt("你是一个助手。")
                .memory(new InMemoryMemory())
                .build();

        // 进行一轮对话
        agent.call(Msg.builder().textContent("我叫张三，在做电商项目").build()).block();

        // 查看当前消息历史数量
        List<Msg> history = agent.getMemory().getMessages();
        System.out.println("当前消息数：" + history.size());

        // 向记忆里注入一条系统消息（比如恢复上次会话的结论）
        agent.getMemory().addMessage(
                Msg.builder()
                        .role(MsgRole.SYSTEM)
                        .content(List.of(TextBlock.builder()
                                .text("用户在上次会话中已确认采用方案 A：Redis 分布式锁。")
                                .build()))
                        .build()
        );
        System.out.println("注入系统消息后，消息数：" + agent.getMemory().getMessages().size());

        // 验证注入的上下文是否被 Agent 感知
        Msg r1 = agent.call(
                Msg.builder().textContent("我们上次讨论的解决方案是什么").build()
        ).block();
        System.out.println("[有历史上下文] " + r1.getTextContent());

        // 清空记忆，开启全新会话
        agent.getMemory().clear();
        System.out.println("清空后，消息数：" + agent.getMemory().getMessages().size());

        // 清空后 Agent 已不记得任何内容
        Msg r2 = agent.call(
                Msg.builder().textContent("我们上次讨论的解决方案是什么").build()
        ).block();
        System.out.println("[清空后] " + r2.getTextContent());
        // 预期：Agent 表示不知道，因为记忆已清空
    }
}