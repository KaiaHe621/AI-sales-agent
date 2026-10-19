package com.jichi.springaialibaba.controller.error;

import com.jichi.springaialibaba.service.SafeChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/safe")
public class SafeChatController {

    private final SafeChatService safeChatService;

    public SafeChatController(SafeChatService safeChatService) {
        this.safeChatService = safeChatService;
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return safeChatService.safeChat(message);
    }
}