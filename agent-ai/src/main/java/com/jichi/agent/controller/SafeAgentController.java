package com.jichi.agent.controller;

import com.jichi.agent.advisor.AgentMaxIterationsException;
import com.jichi.agent.service.PersonalAssistantAgent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/agent")
public class SafeAgentController {

    private final PersonalAssistantAgent agent;

    public SafeAgentController(PersonalAssistantAgent agent) {
        this.agent = agent;
    }

    @PostMapping("/chat")
    public ResponseEntity<AgentResponse> chat(@RequestBody ChatRequest request) {
        try {
            // 套一层 CompletableFuture，统一控制整体任务超时
            String result = CompletableFuture
                    .supplyAsync(() -> agent.chat(request.message()))
                    .get(60, TimeUnit.SECONDS);
            return ResponseEntity.ok(AgentResponse.success(result));

        } catch (AgentMaxIterationsException e) {
            return ResponseEntity.ok(AgentResponse.error(
                    "这个问题比较复杂，建议拆分成几个小问题分别来问",
                    "MAX_ITERATIONS"));

        } catch (TimeoutException e) {
            return ResponseEntity.ok(AgentResponse.error(
                    "处理时间较长，已超出等待上限，请稍后重试或尝试更简单的问题",
                    "TIMEOUT"));

        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof AgentMaxIterationsException) {
                return ResponseEntity.ok(AgentResponse.error(
                        "这个问题比较复杂，建议拆分成几个小问题分别来问",
                        "MAX_ITERATIONS"));
            }
            if (cause.getMessage() != null && cause.getMessage().contains("Token 预算")) {
                return ResponseEntity.ok(AgentResponse.error(
                        "这个问题处理量超出单次限额，请拆分成更小的问题",
                        "BUDGET_EXCEEDED"));
            }
            return ResponseEntity.status(500)
                    .body(AgentResponse.error("服务暂时开了个小差，请稍后重试", "INTERNAL_ERROR"));
        }
    }

    record ChatRequest(String message) {}

    record AgentResponse(boolean success, String content, String errorCode) {
        static AgentResponse success(String content) {
            return new AgentResponse(true, content, null);
        }
        static AgentResponse error(String message, String code) {
            return new AgentResponse(false, message, code);
        }
    }
}