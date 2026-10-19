package com.jichi.eval.controller;

import com.jichi.eval.agent.AgentEvalService;
import com.jichi.eval.agent.AgentTraceEvaluator;
import com.jichi.eval.model.EvalCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eval/agent")
@RequiredArgsConstructor
public class AgentEvalController {

    private final AgentEvalService agentEvalService;

    /**
     * 对单个 Agent 任务做 Trace 评估
     */
    @PostMapping("/trace")
    public AgentTraceEvaluator.TraceEvalResult evaluate(
            @RequestParam String agentEndpoint,
            @RequestBody AgentEvalRequest request
    ) {
        EvalCase evalCase = new EvalCase();
        evalCase.setId(request.getCaseId());
        evalCase.setInput(request.getInput());
        evalCase.setExpectedOutput(request.getExpectedOutput());

        return agentEvalService.evaluate(
                agentEndpoint,
                evalCase,
                request.getExpectedTools()
        );
    }

    @lombok.Data
    public static class AgentEvalRequest {
        private String caseId;
        private String input;
        private String expectedOutput;
        private List<String> expectedTools;
    }
}