package com.jichi.springaialibaba.controller.parallel;

import com.jichi.springaialibaba.service.ParallelChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/parallel")
public class ParallelChatController {

    private final ParallelChatService parallelChatService;

    public ParallelChatController(ParallelChatService parallelChatService) {
        this.parallelChatService = parallelChatService;
    }

    @GetMapping
    public Map<String, String> chat(@RequestParam String question) throws Exception {
        return parallelChatService.parallelChat(question);
    }

}