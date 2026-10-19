package com.jichi.a2a.client;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.*;

@Component
public class TaskResultStore {

    private final Map<String, Object> results = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Object>> waiters = new ConcurrentHashMap<>();

    public void store(String taskId, Object result) {
        results.put(taskId, result);
    }

    public void storeFailed(String taskId, String errorMessage) {
        results.put(taskId, new TaskFailure(errorMessage));
    }

    public void notifyWaiters(String taskId) {
        CompletableFuture<Object> future = waiters.remove(taskId);
        if (future != null) {
            Object result = results.get(taskId);
            if (result instanceof TaskFailure failure) {
                future.completeExceptionally(new RuntimeException(failure.message()));
            } else {
                future.complete(result);
            }
        }
    }

    /**
     * 等待任务完成（通过 Webhook 通知触发）
     */
    public CompletableFuture<Object> waitForResult(String taskId) {
        // 先检查是否已经有结果了（通知可能先到）
        Object existing = results.get(taskId);
        if (existing != null) {
            if (existing instanceof TaskFailure failure) {
                return CompletableFuture.failedFuture(new RuntimeException(failure.message()));
            }
            return CompletableFuture.completedFuture(existing);
        }

        // 还没有结果，注册一个 Future 等待通知
        CompletableFuture<Object> future = new CompletableFuture<>();
        waiters.put(taskId, future);
        return future;
    }

    record TaskFailure(String message) {}
}