package com.jichi.voiceshopping.service;

import tools.jackson.databind.ObjectMapper;
import com.jichi.voiceshopping.agent.AgentFactory;
import com.jichi.voiceshopping.dto.ClarifyResult;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClarifyService {

    private final AgentFactory factory;
    private final ClarifyRuleService ruleService;
    private final ObjectMapper mapper;

    public ClarifyResult decide(String sessionId, String utterance, Map<String, Object> slots) {
        String category = (String) slots.get("category");
        List<String> missing = ruleService.missingSlots(category, slots);

        if (missing.isEmpty()) {
            return ClarifyResult.ready();
        }

        // 兜底：一次最多问 2 个字段，超过就只把最重要的两个送进 Prompt
        // missingSlots 的返回顺序已经是按优先级排好的（required 在前，niceToHave 在后）
        if (missing.size() > 2) {
            missing = missing.subList(0, 2);
        }

        ReActAgent agent = factory.get(sessionId).clarify();
        String userMsg = String.format(
                "用户原话：%s%n已知信息：%s%n缺失字段：%s",
                utterance, writeJson(slots), missing);

        Msg resp = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent(userMsg)
                .build()).block();

        return ClarifyResult.ask(resp.getTextContent().trim(), missing);
    }

    private String writeJson(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (Exception e) { return String.valueOf(o); }
    }
}