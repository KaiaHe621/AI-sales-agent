package com.jichi.agentscope.hook;

import com.jichi.agentscope.service.ApprovalService;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreActingEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;

@Component
@Slf4j
public class HumanApprovalHook implements Hook {

    // 高风险工具列表
    private static final Set<String> HIGH_RISK_TOOLS = Set.of(
            "delete_record", "send_external_email", "transfer_money", "batch_update"
    );

    private final ApprovalService approvalService;

    public HumanApprovalHook(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @Override
    public int priority() {
        return 10;  // 最高优先级，确保审批在所有其他 Hook 之前
    }

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (!(event instanceof PreActingEvent e)) {
            return Mono.just(event);
        }

        String toolName = e.getToolUse().getName();

        if (!HIGH_RISK_TOOLS.contains(toolName)) {
            return Mono.just(event);   // 普通工具直接放行
        }

        // 高风险工具：等待人工审批（同步等待）
        log.warn("[Human-in-the-Loop] 高风险工具 {} 需要审批 | 参数：{}",
                toolName, e.getToolUse().getInput());

        boolean approved = approvalService.requestApproval(
                e.getAgent().getName(),
                toolName,
                e.getToolUse().getInput().toString(),
                30   // 超时 30 秒
        );

        if (approved) {
            log.info("[Human-in-the-Loop] 已批准：{}", toolName);
            return Mono.just(event);
        } else {
            log.warn("[Human-in-the-Loop] 已拒绝：{}", toolName);
            // 修改工具结果，让工具"返回"拒绝信息，Agent 会收到并据此回复用户
            e.getToolUse().getInput().put("__cancelled__", true);
            return Mono.error(new RuntimeException("工具调用 " + toolName + " 已被人工拒绝"));
        }
    }
}