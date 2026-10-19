package com.jichi.springaialibaba.controller.error;

import com.jichi.springaialibaba.service.ManualRetryChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manual-retry")
public class ManualRetryChatController {

    private final ManualRetryChatService manualRetryChatService;

    public ManualRetryChatController(ManualRetryChatService manualRetryChatService) {
        this.manualRetryChatService = manualRetryChatService;
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return manualRetryChatService.chatWithManualRetry(message);
    }
}