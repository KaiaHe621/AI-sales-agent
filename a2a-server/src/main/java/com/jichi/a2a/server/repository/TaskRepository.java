package com.jichi.a2a.server.repository;

import com.jichi.a2a.server.model.Task;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskRepository {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public void save(Task task) {
        tasks.put(task.id(), task);
    }

    public Task findById(String taskId) {
        return tasks.get(taskId);
    }
}