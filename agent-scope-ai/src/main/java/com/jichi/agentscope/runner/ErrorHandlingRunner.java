package com.jichi.agentscope.runner;

import com.jichi.agentscope.tool.DatabaseQueryTool;
import com.jichi.agentscope.tool.NotificationTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

//@Component
public class ErrorHandlingRunner implements ApplicationRunner {

    private final DashScopeChatModel model;
    private final DatabaseQueryTool dbQueryTool;
    private final NotificationTool notificationTool;

    public ErrorHandlingRunner(DashScopeChatModel model,
                               DatabaseQueryTool dbQueryTool,
                               NotificationTool notificationTool) {
        this.model = model;
        this.dbQueryTool = dbQueryTool;
        this.notificationTool = notificationTool;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 测试策略一：LLM 会收到错误信息并告知用户
        Toolkit toolkit1 = new Toolkit();
        toolkit1.registerTool(dbQueryTool);

        ReActAgent agent1 = ReActAgent.builder()
                .name("数据库助手").model(model)
                .sysPrompt("你可以查询数据库，只支持 SELECT。")
                .toolkit(toolkit1).build();

        Msg r1 = agent1.call(
                Msg.builder().textContent("执行 DELETE FROM orders WHERE id=1").build()
        ).block();
        System.out.println("[策略一] " + r1.getTextContent());
        // 预期：LLM 收到"仅支持 SELECT 查询"后，告知用户无法执行该操作

        // 测试策略二：发给不存在的用户，Agent 循环被终止
        Toolkit toolkit2 = new Toolkit();
        toolkit2.registerTool(notificationTool);

        ReActAgent agent2 = ReActAgent.builder()
                .name("通知助手").model(model)
                .sysPrompt("你可以发送系统通知。")
                .toolkit(toolkit2).build();

        Msg r2 = agent2.call(
                Msg.builder().textContent("给用户 user-999 发通知：系统升级中").build()
        ).block();
        System.out.println("[策略二] " + r2.getTextContent());
        // 预期：LLM 收到 error 后，告知用户该用户不存在
    }
}