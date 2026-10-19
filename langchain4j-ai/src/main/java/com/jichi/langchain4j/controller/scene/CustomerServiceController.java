package com.jichi.langchain4j.controller.scene;

import com.jichi.langchain4j.service.scene.SceneAwareAgentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/customer-service")
public class CustomerServiceController {

    // Mock token → userId 映射，生产中替换为真实 JWT 解析
    private static final Map<String, String> TOKEN_USER_MAP = Map.of(
            "token-admin", "admin-001",
            "token-user1", "user-001",
            "token-user2", "user-002"
    );

    private final SceneAwareAgentService agentService;

    public CustomerServiceController(SceneAwareAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(
            @RequestBody ChatRequest request,
            @RequestHeader("X-Session-Id") String sessionId,
            @RequestHeader("X-User-Token") String token) {

        // 第一步：从 token 拿 userId（不信任前端传的角色参数）
        String userId = TOKEN_USER_MAP.getOrDefault(token, "unknown");

        // 第二步：关键词识别场景（生产里建议用分类模型，准确率更高）
        String scene = detectScene(request.message());

        // 第三步：用对应场景的工具集处理
        String reply = agentService.chat(sessionId, scene, request.message());

        return new ChatResponse(reply, scene, userId);
    }

    // 简单关键词识别兜底
    private String detectScene(String message) {
        if (message.contains("退") || message.contains("换") || message.contains("售后") || message.contains("物流")) {
            return "after_sale";
        }
        if (message.contains("投诉") || message.contains("曝光") || message.contains("差评")) {
            return "complaint";
        }
        return "pre_sale";
    }

    record ChatRequest(String message) {
    }

    record ChatResponse(String reply, String scene, String userId) {
    }
}