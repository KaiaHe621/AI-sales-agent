package com.jichi.springaialibaba.controller.error;

import com.jichi.springaialibaba.service.ResilientChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resilient")
public class ResilientChatController {

    private final ResilientChatService resilientChatService;

    public ResilientChatController(ResilientChatService resilientChatService) {
        this.resilientChatService = resilientChatService;
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return resilientChatService.chat(message);
    }
}