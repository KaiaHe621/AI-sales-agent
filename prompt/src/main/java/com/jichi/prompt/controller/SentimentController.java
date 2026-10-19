package com.jichi.prompt.controller;

import com.jichi.prompt.service.SentimentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {

    private final SentimentService sentimentService;

    public SentimentController(SentimentService sentimentService) {
        this.sentimentService = sentimentService;
    }

    @GetMapping
    public String analyze(@RequestParam String comment) {
        return sentimentService.analyze(comment);
    }

    @PostMapping("/batch")
    public Map<String, String> analyzeBatch(@RequestBody List<String> comments) {
        return sentimentService.analyzeBatch(comments);
    }
}