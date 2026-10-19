package com.jichi.eval.controller;

import com.jichi.eval.agent.AgentTrace;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agent")
public class MockAgentController {

    @PostMapping("/run")
    public AgentTrace run(@RequestBody Map<String, String> request) {
        String input = request.getOrDefault("input", "");

        return AgentTrace.builder()
                .traceId("trace-" + System.currentTimeMillis())
                .input(input)
                .finalAnswer("订单 ORD-001 已发货，预计明天送达。")
                .totalSteps(3)
                .toolCallCount(1)
                .steps(List.of(
                        AgentTrace.TraceStep.builder()
                                .stepIndex(0)
                                .type(AgentTrace.TraceStep.StepType.THOUGHT)
                                .content("用户想查物流状态，需要调用订单查询工具")
                                .build(),
                        AgentTrace.TraceStep.builder()
                                .stepIndex(1)
                                .type(AgentTrace.TraceStep.StepType.ACTION)
                                .toolName("query_order_by_id")
                                .toolInput("{\"orderId\": \"ORD-001\"}")
                                .content("调用订单查询工具")
                                .build(),
                        AgentTrace.TraceStep.builder()
                                .stepIndex(2)
                                .type(AgentTrace.TraceStep.StepType.FINAL_ANSWER)
                                .content("订单 ORD-001 已发货，预计明天送达。")
                                .build()
                ))
                .build();
    }
}