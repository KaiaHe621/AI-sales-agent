package com.jichi.agentscope.service;

import com.jichi.agentscope.model.ApprovalItem;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ApprovalService {

    // 正在等待审批的工单：approvalId -> Future<是否批准>
    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> pending =
            new ConcurrentHashMap<>();

    // 用于前端轮询展示，保存工单详情
    private final ConcurrentHashMap<String, ApprovalItem> pendingItems =
            new ConcurrentHashMap<>();

    /**
     * Hook 调用此方法阻塞等待审批结果。
     * timeoutSeconds 内无人操作则超时自动拒绝。
     */
    public boolean requestApproval(String agentName, String toolName,
                                   String toolInput, int timeoutSeconds) {
        String approvalId = UUID.randomUUID().toString().substring(0, 8);
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        pending.put(approvalId, future);
        pendingItems.put(approvalId, new ApprovalItem(
                approvalId, agentName, toolName, toolInput, System.currentTimeMillis()
        ));

        System.out.printf("[审批等待] approvalId=%s 工具=%s 参数=%s%n",
                approvalId, toolName, toolInput);

        try {
            // 阻塞当前线程，等待管理员操作或超时
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.printf("[审批超时] approvalId=%s%n", approvalId);
            return false;
        } catch (Exception e) {
            return false;
        } finally {
            pending.remove(approvalId);
            pendingItems.remove(approvalId);
        }
    }

    /** 管理员批准 */
    public boolean approve(String approvalId) {
        CompletableFuture<Boolean> future = pending.get(approvalId);
        if (future == null) return false;
        future.complete(true);
        return true;
    }

    /** 管理员拒绝 */
    public boolean reject(String approvalId) {
        CompletableFuture<Boolean> future = pending.get(approvalId);
        if (future == null) return false;
        future.complete(false);
        return true;
    }

    /** 前端轮询用：获取当前所有待审批工单 */
    public Collection<ApprovalItem> listPending() {
        return pendingItems.values();
    }
}