package com.jichi.langchain4j.service.ownerAgent;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AsyncAnalysisService {

    private final AnalysisAgent agent;
    private final Map<String, TaskStatus> taskMap = new ConcurrentHashMap<>();

    public AsyncAnalysisService(AnalysisAgent agent) {
        this.agent = agent;
    }

    public String submitTask(String question) {
        String taskId = UUID.randomUUID().toString();
        taskMap.put(taskId, new TaskStatus("RUNNING", null, null));

        CompletableFuture.supplyAsync(() -> agent.analyze(taskId, question))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        taskMap.put(taskId, new TaskStatus("FAILED", null, ex.getMessage()));
                    } else {
                        taskMap.put(taskId, new TaskStatus("DONE", result, null));
                    }
                });

        return taskId;
    }

    public TaskStatus getStatus(String taskId) {
        return taskMap.getOrDefault(taskId, new TaskStatus("NOT_FOUND", null, null));
    }

    public record TaskStatus(String status, String result, String error) {
    }
}