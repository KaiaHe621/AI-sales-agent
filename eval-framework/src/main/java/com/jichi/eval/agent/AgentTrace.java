package com.jichi.eval.agent;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgentTrace {

    private String traceId;
    private String input;
    private String finalAnswer;
    private List<TraceStep> steps;
    private int totalSteps;
    private int toolCallCount;

    @Data
    @Builder
    public static class TraceStep {
        private int stepIndex;
        private StepType type;
        private String content;
        private String toolName;
        private String toolInput;

        public enum StepType {
            THOUGHT, ACTION, OBSERVATION, FINAL_ANSWER
        }
    }
}