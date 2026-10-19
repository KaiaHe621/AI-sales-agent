package com.jichi.eval.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.eval.model.EvalCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEvalService {

    private final AgentTraceEvaluator traceEvaluator;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 调用 Agent 接口，获取 Trace，执行评估
     *
     * @param agentTraceEndpoint  Agent 接口地址，返回格式为 AgentTrace JSON
     * @param evalCase            测试用例
     * @param expectedTools       期望调用的工具列表
     */
    public AgentTraceEvaluator.TraceEvalResult evaluate(
            String agentTraceEndpoint,
            EvalCase evalCase,
            List<String> expectedTools) {

        try {
            Map<String, String> request = Map.of("input", evalCase.getInput());
            Map<?, ?> response = restTemplate.postForObject(
                    agentTraceEndpoint, request, Map.class);

            if (response == null) {
                throw new RuntimeException("Agent 返回空响应");
            }

            AgentTrace trace = objectMapper.convertValue(response, AgentTrace.class);
            log.info("[{}] Agent 执行完成，共 {} 步，调用工具 {} 次",
                    evalCase.getId(), trace.getTotalSteps(), trace.getToolCallCount());

            return traceEvaluator.evaluate(evalCase, trace, expectedTools);

        } catch (Exception e) {
            log.error("[{}] Agent 评估失败：{}", evalCase.getId(), e.getMessage());
            return AgentTraceEvaluator.TraceEvalResult.builder()
                    .traceId("error")
                    .caseId(evalCase.getId())
                    .toolAccuracyScore(0.0)
                    .efficiencyScore(0.0)
                    .answerQualityScore(0.0)
                    .overallScore(0.0)
                    .actualToolCalls(List.of())
                    .expectedToolCalls(expectedTools)
                    .build();
        }
    }
}