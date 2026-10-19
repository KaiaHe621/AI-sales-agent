package com.jichi.agentscope.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.agentscope.core.message.Msg;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/pipeline")
@RequiredArgsConstructor
public class ResearchPipelineController {

    // 注入 ResearchPipelineConfig 里声明的 Bean
    private final ParallelAgent parallelResearchAgent;

    @GetMapping("/research")
    public Map<String, String> research(@RequestParam String topic) throws Exception {
        Optional<OverAllState> result = parallelResearchAgent.invoke(topic);
        if (result.isEmpty()) {
            return Map.of("error", "pipeline 返回空结果");
        }
        OverAllState state = result.get();
        return Map.of(
                "topic",  topic,
                "report", extractText(state.value("research_report"))
        );
    }

    private String extractText(Object value) {
        if (value == null) return "";
        if (value instanceof String s) return s;
        if (value instanceof Msg msg) return msg.getTextContent();
        return value.toString();
    }
}