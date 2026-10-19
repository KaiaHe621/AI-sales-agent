package com.jichi.prompt.controller;

import com.jichi.prompt.entity.SafeChatRequest;
import com.jichi.prompt.service.UserRateLimiter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/safe-chat")
public class RateLimitedChatController {

    private final UserRateLimiter rateLimiter;

    public RateLimitedChatController(UserRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/ask")
    public ResponseEntity<String> chat(@RequestBody SafeChatRequest req,
                                        @RequestHeader("X-User-Id") String userId) {
        if (!rateLimiter.tryAcquire(userId)) {
            return ResponseEntity.status(429).body("请求过于频繁，请稍后再试");
        }
        return ResponseEntity.ok("正常处理中...");
    }
}