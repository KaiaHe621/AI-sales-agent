package com.jichi.agent.support;

import com.jichi.agent.support.NotificationService;
import com.jichi.agent.support.PendingApprovalRepository;
import com.jichi.agent.support.PendingApprovalRepository.PendingApproval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class HumanInTheLoopService {

    private static final Logger log = LoggerFactory.getLogger(HumanInTheLoopService.class);

    private final PendingApprovalRepository approvalRepo;
    private final NotificationService notificationService;

    public HumanInTheLoopService(PendingApprovalRepository approvalRepo,
                                  NotificationService notificationService) {
        this.approvalRepo = approvalRepo;
        this.notificationService = notificationService;
    }

    /**
     * 高风险操作暂停，等待人工审批
     * 触发场景：大额退款、批量操作、敏感数据修改
     */
    public void requestApproval(String workflowId, String description,
                                  String approverEmail, Object payload) {
        PendingApproval approval = new PendingApproval(
                workflowId, description, approverEmail,
                payload, "PENDING", null, LocalDateTime.now());

        approvalRepo.save(approval);

        // 通知审批人——这一步是确定性的，必须发
        notificationService.notifyApprover(approverEmail,
                "需要你审批：" + description +
                "\n审批链接：https://admin/approve/" + workflowId);
    }

    /**
     * 审批通过，恢复流程继续执行
     */
    public void onApproved(String workflowId) {
        approvalRepo.updateStatus(workflowId, "APPROVED", null);
        // 实际项目这里触发工作流引擎恢复（Activiti / Flowable 等）
        // 教学场景简化：记录状态，由轮询接口驱动后续
        log.info("[审批] workflowId={} 已通过，等待后续流程恢复", workflowId);
    }

    /**
     * 审批拒绝，终止流程并通知用户
     */
    public void onRejected(String workflowId, String reason) {
        approvalRepo.updateStatus(workflowId, "REJECTED", reason);
        log.info("[审批] workflowId={} 已拒绝，原因：{}", workflowId, reason);
    }

    /** 查询审批状态（供轮询） */
    public String getStatus(String workflowId) {
        return approvalRepo.findById(workflowId)
                .map(PendingApproval::status)
                .orElse("NOT_FOUND");
    }
}