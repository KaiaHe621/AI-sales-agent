package com.jichi.prompt.controller;

import com.jichi.prompt.service.SelfConsistencyService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/self-consistency")
public class SelfConsistencyController {

    private final SelfConsistencyService selfConsistencyService;

    public SelfConsistencyController(SelfConsistencyService selfConsistencyService) {
        this.selfConsistencyService = selfConsistencyService;
    }

    @GetMapping("/query")
    public String query(@RequestParam String question,
                        @RequestParam(defaultValue = "5") int sampleCount) throws Exception {
        return selfConsistencyService.query(question, sampleCount);
    }

}