package com.jichi.agentscope.controller;

import com.jichi.agentscope.context.UserContextHolder;
import com.jichi.agentscope.hook.ContextInjectionHook;
import com.jichi.agentscope.model.ChatRequest;
import com.jichi.agentscope.model.ChatResponse;
import com.jichi.agentscope.tool.SalesQueryTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final DashScopeChatModel model;
    private final SalesQueryTool salesTool;  // @Component 注入

    // HTTP 头部只允许 ASCII，角色和区域用英文 code 传入，Controller 内翻译成中文描述
    private static final Map<String, String> ROLE_MAP = Map.of(
            "sales_manager", "销售经理",
            "sales_staff",   "销售专员",
            "admin",         "管理员",
            "guest",         "访客"
    );

    private static final Map<String, String> REGION_MAP = Map.of(
            "east",     "华东",
            "south",    "华南",
            "north",    "华北",
            "west",     "西部",
            "national", "全国"
    );

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestHeader(value = "X-User-Id",     defaultValue = "anonymous") String userId,
            @RequestHeader(value = "X-User-Role",   defaultValue = "guest")     String userRoleCode,
            @RequestHeader(value = "X-User-Region", defaultValue = "national")  String userRegionCode,
            @RequestBody ChatRequest request
    ) {
        // 1. 将 ASCII code 翻译为中文描述后写入 ThreadLocal
        String roleDesc   = ROLE_MAP.getOrDefault(userRoleCode,   userRoleCode);
        String regionDesc = REGION_MAP.getOrDefault(userRegionCode, userRegionCode);
        String contextDesc = String.format(
                "用户ID：%s，角色：%s，负责区域：%s",
                userId, roleDesc, regionDesc
        );
        UserContextHolder.set(contextDesc);

        try {
            // 2. 每次请求创建独立的 Agent 实例（Agent 有状态，不能共享）
            Toolkit toolkit = new Toolkit();
            toolkit.registerTool(salesTool);

            ReActAgent agent = ReActAgent.builder()
                    .name("销售助手")
                    .model(model)
                    .sysPrompt("你是一个销售数据助手，根据用户身份提供对应权限范围内的数据。")
                    .toolkit(toolkit)
                    .hooks(List.of(new ContextInjectionHook()))
                    .build();

            // 3. 发起调用
            Msg response = agent.call(
                    Msg.builder().textContent(request.message()).build()
            ).block();

            return ResponseEntity.ok(new ChatResponse(response.getTextContent()));

        } finally {
            // 4. 必须清理 ThreadLocal，防止线程池中的线程污染下一个请求
            UserContextHolder.clear();
        }
    }
}