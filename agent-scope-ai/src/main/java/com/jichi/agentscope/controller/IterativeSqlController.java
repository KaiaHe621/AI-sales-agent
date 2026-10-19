package com.jichi.agentscope.controller;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import io.agentscope.core.message.Msg;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/pipeline")
@RequiredArgsConstructor
public class IterativeSqlController {

    // 注入 IterativeSqlConfig 里声明的 Bean
    private final LoopAgent iterativeSqlAgent;

    @GetMapping("/sql/iterative")
    public Map<String, String> iterativeSql(@RequestParam String query) throws Exception {
        Optional<OverAllState> result = iterativeSqlAgent.invoke(query);
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