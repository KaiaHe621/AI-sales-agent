package com.jichi.agentscope.controller;

import com.jichi.agentscope.hook.HumanApprovalHook;
import com.jichi.agentscope.model.ChatRequest;
import com.jichi.agentscope.model.ChatResponse;
import com.jichi.agentscope.service.ApprovalService;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final DashScopeChatModel model;
    private final HumanApprovalHook approvalHook;
    private final ApprovalService approvalService;

    // 高风险工具（Mock 实现，直接返回固定内容）
    @Component
    static class RiskyOperationTool {

        @Tool(name = "delete_record", description = "删除指定 ID 的业务记录，高风险操作")
        public String deleteRecord(
                @ToolParam(name = "record_id", description = "要删除的记录 ID") String recordId
        ) {
            // 真实项目里这里调数据库删除
            return "记录 " + recordId + " 已删除";
        }

        @Tool(name = "transfer_money", description = "转账操作，高风险")
        public String transferMoney(
                @ToolParam(name = "amount", description = "转账金额（元）") double amount,
                @ToolParam(name = "to_account", description = "收款账号") String toAccount
        ) {
            return String.format("已向 %s 转账 %.2f 元", toAccount, amount);
        }
    }

    // ----------------------------------------------------------------
    // 1. 发起 Agent 任务（可能因高风险工具而中途阻塞等待审批）
    // ----------------------------------------------------------------
    @PostMapping("/agent-chat")
    public ResponseEntity<ChatResponse> agentChat(@RequestBody ChatRequest request) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new RiskyOperationTool());

        ReActAgent agent = ReActAgent.builder()
                .name("运营助手")
                .model(model)
                .sysPrompt("你是运营助手，可以执行删除记录、转账等操作，执行前系统会进行安全审核。")
                .toolkit(toolkit)
                .hooks(List.of(approvalHook))
                .build();

        try {
            Msg response = agent.call(
                    Msg.builder().textContent(request.message()).build()
            ).block();
            return ResponseEntity.ok(new ChatResponse(response.getTextContent()));
        } catch (Exception e) {
            // Hook 拒绝时抛出异常，这里捕获并返回友好提示
            return ResponseEntity.ok(new ChatResponse("操作已被安全审核拒绝：" + e.getMessage()));
        }
    }

    // ----------------------------------------------------------------
    // 2. 查询当前待审批的工单（前端轮询此接口）
    // ----------------------------------------------------------------
    @GetMapping("/approvals/pending")
    public ResponseEntity<?> listPending() {
        return ResponseEntity.ok(approvalService.listPending());
    }

    // ----------------------------------------------------------------
    // 3. 管理员批准
    // ----------------------------------------------------------------
    @PostMapping("/approvals/{approvalId}/approve")
    public ResponseEntity<String> approve(@PathVariable String approvalId) {
        boolean ok = approvalService.approve(approvalId);
        return ok
                ? ResponseEntity.ok("已批准：" + approvalId)
                : ResponseEntity.badRequest().body("审批单不存在或已超时：" + approvalId);
    }

    // ----------------------------------------------------------------
    // 4. 管理员拒绝
    // ----------------------------------------------------------------
    @PostMapping("/approvals/{approvalId}/reject")
    public ResponseEntity<String> reject(@PathVariable String approvalId) {
        boolean ok = approvalService.reject(approvalId);
        return ok
                ? ResponseEntity.ok("已拒绝：" + approvalId)
                : ResponseEntity.badRequest().body("审批单不存在或已超时：" + approvalId);
    }
}