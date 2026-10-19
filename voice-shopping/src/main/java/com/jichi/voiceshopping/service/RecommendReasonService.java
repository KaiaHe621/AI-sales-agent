package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.agent.AgentMemoryPolicy;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.jichi.voiceshopping.agent.AgentFactory;
import com.jichi.voiceshopping.dto.RecommendedItem;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendReasonService {

    private final AgentFactory factory;
    private final ObjectMapper mapper;
    private final AgentMemoryPolicy memoryPolicy;

    public List<RecommendedItem> attachReasons(String sessionId,
                                               String userNeeds,
                                               List<RecommendedItem> products) {
        if (products.isEmpty()) return products;
        ReActAgent agent = factory.get(sessionId).recommend();
        memoryPolicy.beforeRecommendCall(agent);

        Map<String, Object> input = Map.of(
                "userNeeds", userNeeds,
                "products", products.stream().map(p -> Map.of(
                        "productId", p.productId(),
                        "name", p.name(),
                        "price", p.price(),
                        "attributes", p.attributes()
                )).toList()
        );

        try {
            Msg resp = agent.call(Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(mapper.writeValueAsString(input))
                    .build()).block();

            String json = extractJson(resp.getTextContent());
            List<Map<String, Object>> out = mapper.readValue(json, new TypeReference<>() {});
            Map<Long, String> reasonMap = new HashMap<>();
            for (Map<String, Object> r : out) {
                Long pid = ((Number) r.get("productId")).longValue();
                reasonMap.put(pid, (String) r.get("reason"));
            }
            return products.stream()
                    .map(p -> p.withReason(reasonMap.getOrDefault(p.productId(), "")))
                    .toList();
        } catch (Exception e) {
            log.warn("推荐理由生成失败，降级为空理由", e);
            return products;
        }
    }

    private String extractJson(String text) {
        int s = text.indexOf('[');
        int e = text.lastIndexOf(']');
        if (s >= 0 && e > s) return text.substring(s, e + 1);
        return text;
    }
}