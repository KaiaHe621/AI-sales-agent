package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.agent.AgentMemoryPolicy;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.jichi.voiceshopping.agent.AgentFactory;
import com.jichi.voiceshopping.dto.EmotionResult;
import com.jichi.voiceshopping.dto.RecommendResult;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmotionService {

    private final AgentFactory factory;
    private final SessionMoodDetector moodDetector;
    private final ObjectMapper mapper;
    private final AgentMemoryPolicy memoryPolicy;

    public EmotionResult wrap(String sessionId, String userUtterance,
                              String userNeeds, RecommendResult rec) {
        String mood = moodDetector.detect(sessionId, userUtterance);

        Map<String, Object> input = Map.of(
                "userUtterance", userUtterance,
                "sessionMood", mood,
                "userNeeds", userNeeds,
                "products", rec.items()
        );

        ReActAgent agent = factory.get(sessionId).emotion();
        try {
            Msg resp = agent.call(Msg.builder()
                    .role(MsgRole.USER)
                    .textContent(mapper.writeValueAsString(input))
                    .build()).block();

            // prompt 改成纯文本，不再走 JSON 解析；sanitize 兜一下偶发的 ```json``` 包裹
            String speech = sanitize(resp == null ? null : resp.getTextContent());
            if (speech == null || speech.isBlank()) {
                log.warn("EmotionAgent 返回空，降级为模板文案");
                return new EmotionResult(fallback(rec), rec.items());
            }
            return new EmotionResult(speech, rec.items());
        } catch (Exception e) {
            log.warn("EmotionAgent 调用失败，降级为模板文案", e);
            return new EmotionResult(fallback(rec), rec.items());
        }
    }

    /** 去掉 ```json ... ``` 之类的 Markdown 包裹，模型偶尔会犯这个毛病 */
    private static String sanitize(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        s = s.replaceAll("(?s)^```(?:json|text)?\\s*", "")
                .replaceAll("(?s)\\s*```$", "");
        return s.trim();
    }

    private String fallback(RecommendResult rec) {
        if (rec.items().isEmpty()) {
            return "鸡哥帮你找了一下，这个条件下合适的不多，要不要放宽点预算再看看？";
        }
        StringBuilder sb = new StringBuilder("好，鸡哥给你挑了几款。");
        int i = 1;
        for (var item : rec.items()) {
            sb.append("第").append(i++).append("款：").append(item.name()).append("，")
              .append(item.reason() == null ? "" : item.reason()).append("。");
        }
        sb.append("你看看选哪个？");
        return sb.toString();
    }

    private String extractJson(String text) {
        int s = text.indexOf('{');
        int e = text.lastIndexOf('}');
        if (s >= 0 && e > s) return text.substring(s, e + 1);
        return text;
    }
}