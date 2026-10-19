package com.jichi.a2a.client;

import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/long-task")
public class LongRunningTaskController {

    private final LongRunningTaskService longRunningTaskService;

    public LongRunningTaskController(LongRunningTaskService longRunningTaskService) {
        this.longRunningTaskService = longRunningTaskService;
    }

    /**
     * 提交长时间任务，接口会阻塞等待 Webhook 通知（Demo 场景）
     * 生产环境建议改成异步接口 + 轮询状态
     */
    @GetMapping("/submit")
    public Map<String, Object> submit(@RequestParam String question) throws Exception {
        String result = longRunningTaskService.submitLongTask(question)
                .get(30, TimeUnit.MINUTES);

        return Map.of("status", "completed", "result", result);
    }
}