package com.jichi.langchain4j.controller.ownerAgent;

import com.jichi.langchain4j.service.ownerAgent.AsyncAnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analysis/async")
public class AsyncAnalysisController {

    private final AsyncAnalysisService asyncService;

    public AsyncAnalysisController(AsyncAnalysisService asyncService) {
        this.asyncService = asyncService;
    }

    @PostMapping("/submit")
    public Map<String, String> submit(@RequestBody Map<String, String> req) {
        String taskId = asyncService.submitTask(req.get("question"));
        return Map.of("taskId", taskId, "message", "任务已提交，请用 taskId 轮询状态");
    }

    @GetMapping("/status/{taskId}")
    public AsyncAnalysisService.TaskStatus getStatus(@PathVariable String taskId) {
        return asyncService.getStatus(taskId);
    }
}