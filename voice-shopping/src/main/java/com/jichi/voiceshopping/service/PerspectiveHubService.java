package com.jichi.voiceshopping.service;

import com.jichi.voiceshopping.agent.AgentFactory;
import com.jichi.voiceshopping.dto.RecommendedItem;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.pipeline.MsgHub;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerspectiveHubService {

    private final AgentFactory agentFactory;

    public String discuss(String sessionId, String utterance, List<RecommendedItem> items) {
        if (items == null || items.isEmpty()) return "";

        AgentFactory.PerspectiveTeam team = agentFactory.newPerspectiveTeam();

        Msg announcement = Msg.builder()
                .name("host")
                .role(MsgRole.USER)
                .textContent("""
                        用户原话：%s
                        
                        待点评的 Top3 商品：
                        %s
                        
                        请各位依次发言，每人 30 字以内。
                        """.formatted(utterance, renderItems(items)))
                .build();

        try (MsgHub hub = MsgHub.builder()
                .name("perspective_" + sessionId)
                .participants(team.priceAdvisor(), team.proRunner(), team.beginnerBuyer())
                .announcement(announcement)
                .enableAutoBroadcast(true)
                .build()) {

            hub.enter().block();

            Msg a = team.priceAdvisor().call().block();
            Msg b = team.proRunner().call().block();
            Msg c = team.beginnerBuyer().call().block();

            return """
                    价格顾问:%s
                    专业跑者:%s
                    入门买家:%s""".formatted(text(a), text(b), text(c));
        } catch (Exception e) {
            log.warn("视角点评团讨论失败，降级为空文案 sessionId={}", sessionId, e);
            return "";
        }
    }

    private String renderItems(List<RecommendedItem> items) {
        return items.stream()
                .map(i -> "- %s / ¥%s / %s".formatted(i.name(), i.price(), i.reason()))
                .collect(Collectors.joining("\n"));
    }

    private String text(Msg m) {
        return m == null ? "" : m.getTextContent();
    }
}