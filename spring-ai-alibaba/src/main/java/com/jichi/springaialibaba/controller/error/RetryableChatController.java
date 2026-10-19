package com.jichi.springaialibaba.controller.error;

import com.jichi.springaialibaba.service.RetryableChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/retry")
public class RetryableChatController {

    private final RetryableChatService retryableChatService;

    public RetryableChatController(RetryableChatService retryableChatService) {
        this.retryableChatService = retryableChatService;
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return retryableChatService.chatWithRetry(message);
    }
}