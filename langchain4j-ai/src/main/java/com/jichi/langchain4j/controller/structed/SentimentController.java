package com.jichi.langchain4j.controller.structed;

import com.jichi.langchain4j.model.SentimentResult;
import com.jichi.langchain4j.service.SentimentAnalyzer;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/structured/sentiment")
public class SentimentController {

    private final SentimentAnalyzer analyzer;

    public SentimentController(SentimentAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    @GetMapping
    public SentimentResult analyze(@RequestParam String review) {
        return analyzer.analyze(review);
    }
}