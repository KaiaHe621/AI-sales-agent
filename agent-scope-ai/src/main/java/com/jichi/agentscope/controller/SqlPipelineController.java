package com.jichi.agentscope.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.agentscope.core.message.Msg;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/pipeline")
@RequiredArgsConstructor
public class SqlPipelineController {

    // 注入 SqlPipelineConfig 里声明的 Bean
    private final SequentialAgent sqlQualityAgent;

    @GetMapping("/sql")
    public Map<String, String> sql(@RequestParam String query) throws Exception {
        Optional<OverAllState> result = sqlQualityAgent.invoke(query);
        if (result.isEmpty()) {
            return Map.of("error", "pipeline 返回空结果");
        }
        OverAllState state = result.get();
        return Map.of(
                "query", query,
                "sql",   extractText(state.value("sql")),
                "score", extractText(state.value("score"))
        );
    }

    private String extractText(Object value) {
        if (value == null) return "";
        if (value instanceof String s) return s;
        if (value instanceof Msg msg) return msg.getTextContent();
        return value.toString();
    }
}