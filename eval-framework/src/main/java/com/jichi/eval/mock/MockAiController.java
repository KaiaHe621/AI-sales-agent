package com.jichi.eval.mock;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/mock")
public class MockAiController {

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        // 简单 mock：复述问题 + 固定回答
        return Map.of("reply", "你问的是：" + message + "。这是一个模拟回答，用于测试 Eval 框架。");
    }
}