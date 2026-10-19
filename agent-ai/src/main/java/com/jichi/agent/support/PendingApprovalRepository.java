package com.jichi.agent.support;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 待审批任务 Mock 仓库 */
@Component
public class PendingApprovalRepository {

    public record PendingApproval(
            String workflowId,
            String description,
            String approver,
            Object payload,
            String status,
            String rejectReason,
            LocalDateTime createdAt) {}

    private final Map<String, PendingApproval> store = new ConcurrentHashMap<>();

    public void save(PendingApproval approval) {
        store.put(approval.workflowId(), approval);
    }

    public Optional<PendingApproval> findById(String workflowId) {
        return Optional.ofNullable(store.get(workflowId));
    }

    /** 更新状态（record 不可变，用新对象替换） */
    public void updateStatus(String workflowId, String status, String rejectReason) {
        store.computeIfPresent(workflowId, (k, old) ->
                new PendingApproval(old.workflowId(), old.description(), old.approver(),
                        old.payload(), status, rejectReason, old.createdAt()));
    }
}