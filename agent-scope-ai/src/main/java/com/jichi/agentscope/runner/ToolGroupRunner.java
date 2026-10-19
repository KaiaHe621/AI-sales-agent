package com.jichi.agentscope.runner;

import com.jichi.agentscope.tool.BatchUpdateTool;
import com.jichi.agentscope.tool.DeleteTool;
import com.jichi.agentscope.tool.QueryTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import java.util.List;

//@Component
public class ToolGroupRunner implements ApplicationRunner {

    private final DashScopeChatModel model;

    public ToolGroupRunner(DashScopeChatModel model) {
        this.model = model;
    }

    @Override
    public void run(ApplicationArguments args) {
        Toolkit toolkit = new Toolkit();
        toolkit.createToolGroup("basic", "基础工具", true);
        toolkit.createToolGroup("admin", "管理员工具", false);
        toolkit.registration().tool(new QueryTool()).group("basic").apply();
        toolkit.registration().tool(new DeleteTool()).tool(new BatchUpdateTool()).group("admin").apply();

        ReActAgent agent = ReActAgent.builder()
                .name("权限助手")
                .model(model)
                .sysPrompt("你是一个业务助手，根据权限提供对应的操作。")
                .toolkit(toolkit)
                .build();

        // 普通用户：只能查询
        Msg r1 = agent.call(
                Msg.builder().textContent("查一下记录 record-001 的详情").build()
        ).block();
        System.out.println("[普通用户] " + r1.getTextContent());

        // 激活管理员工具后：可以删除
        toolkit.updateToolGroups(List.of("admin"), true);
        toolkit.updateToolGroups(List.of("basic"), false);

        Msg r2 = ReActAgent.builder()
                .name("权限助手").model(model)
                .sysPrompt("你是一个业务助手，根据权限提供对应的操作。")
                .toolkit(toolkit).build()
                .call(Msg.builder().textContent("删除记录 record-001").build())
                .block();
        System.out.println("[管理员] " + r2.getTextContent());
    }
}