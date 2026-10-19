package com.jichi.agent.controller;

import com.jichi.agent.service.RefundWorkflowService;
import com.jichi.agent.support.HumanInTheLoopService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflow/refund")
public class RefundWorkflowController {

    private final RefundWorkflowService refundWorkflowService;
    private final HumanInTheLoopService humanInTheLoopService;

    public RefundWorkflowController(RefundWorkflowService refundWorkflowService,
                                     HumanInTheLoopService humanInTheLoopService) {
        this.refundWorkflowService = refundWorkflowService;
        this.humanInTheLoopService = humanInTheLoopService;
    }

    /** 提交退款申请 */
    @PostMapping
    public RefundWorkflowService.WorkflowResult processRefund(
            @RequestBody RefundRequest request) {
        return refundWorkflowService.processRefund(
                request.userId(), request.orderId(), request.message());
    }

    /** 审批通过 */
    @PostMapping("/approve/{workflowId}")
    public String approve(@PathVariable String workflowId) {
        humanInTheLoopService.onApproved(workflowId);
        return "已审批通过：" + workflowId;
    }

    /** 审批拒绝 */
    @PostMapping("/reject/{workflowId}")
    public String reject(@PathVariable String workflowId,
                         @RequestParam String reason) {
        humanInTheLoopService.onRejected(workflowId, reason);
        return "已拒绝：" + workflowId;
    }

    /** 查询审批状态 */
    @GetMapping("/status/{workflowId}")
    public String status(@PathVariable String workflowId) {
        return humanInTheLoopService.getStatus(workflowId);
    }

    record RefundRequest(String userId, String orderId, String message) {}
}