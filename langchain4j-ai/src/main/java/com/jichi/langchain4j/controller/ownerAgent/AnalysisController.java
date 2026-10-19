package com.jichi.langchain4j.controller.ownerAgent;

import com.jichi.langchain4j.service.ownerAgent.AnalysisAgent;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    private final AnalysisAgent analysisAgent;

    public AnalysisController(AnalysisAgent analysisAgent) {
        this.analysisAgent = analysisAgent;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest req) {
        String result = analysisAgent.analyze(req.sessionId(), req.question());
        return Map.of("sessionId", req.sessionId(), "result", result);
    }

    record ChatRequest(String sessionId, String question) {}
}