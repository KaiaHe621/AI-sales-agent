package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.agent.AgentMemoryPolicy;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.jichi.voiceshopping.agent.AgentFactory;
import com.jichi.voiceshopping.dto.Intent;
import com.jichi.voiceshopping.dto.IntentResult;
import com.jichi.voiceshopping.memory.ShortTermMemory;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentService {

    private final AgentFactory factory;
    private final ObjectMapper mapper;
    private final ShortTermMemory memory;
    private final IntentCache cache;
    private final AgentMemoryPolicy memoryPolicy;

    public IntentResult classify(String sessionId, String utterance) {
        // -1) 极短确认词直接走进程内常量，连 Redis 都省了
        if (CommonConfirmer.isCommonConfirmer(utterance)) {
            log.debug("[Intent] 命中 confirmer 短路 sessionId={} utterance={}", sessionId, utterance);
            return CommonConfirmer.CONFIRMER_INTENT;
        }

        // 0) 先查缓存——5 分钟内同一会话说同样的话，跳过 LLM
        IntentResult cached = cache.get(sessionId, utterance);
        if (cached != null) {
            log.debug("[Intent] 命中缓存 sessionId={} utterance={}", sessionId, utterance);
            return cached;
        }

        ReActAgent agent = factory.get(sessionId).intent();
        memoryPolicy.beforeIntentCall(agent);

        String history = memory.recent(sessionId, 3).stream()
                .map(t -> t.role() + ": " + t.text())
                .reduce((a, b) -> a + "\n" + b).orElse("（无历史）");

        String userInput = """
                最近 3 轮对话摘要：
                %s

                当前这一句：
                %s
                """.formatted(history, utterance);

        Msg resp = agent.call(Msg.builder()
                .role(MsgRole.USER)
                .textContent(userInput)
                .build()).block();

        IntentResult result;
        try {
            String json = extractJson(resp.getTextContent());
            Map<String, Object> parsed = mapper.readValue(json, new TypeReference<>() {});
            Intent intent = Intent.valueOf((String) parsed.get("intent"));
            Map<String, Object> slots = (Map<String, Object>) parsed.get("slots");
            double conf = ((Number) parsed.getOrDefault("confidence", 0.5)).doubleValue();
            result = new IntentResult(intent, slots, conf);
        } catch (Exception e) {
            log.warn("意图 JSON 解析失败，降级为 OUT_OF_SCOPE：{}", resp.getTextContent(), e);
            result = new IntentResult(Intent.OUT_OF_SCOPE, Map.of(), 0.3);
        }

        // 1) 解析失败的 OUT_OF_SCOPE 不写缓存，避免一次抖动把所有"相似的话"都污染成越权
        if (result.intent() != Intent.OUT_OF_SCOPE) {
            cache.put(sessionId, utterance, result);
        }
        return result;
    }

    /**
     * LLM 有时会多输出几个字，这里抠出 JSON 部分
     */
    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return text;
    }
}