package com.jichi.a2a.server.skill;

import com.jichi.a2a.server.model.Message;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 支持流式中间状态推送的 Skill 接口
 */
public interface StreamingSkillHandler extends SkillHandler {
    void handleStreaming(String taskId, String sessionId,
                         List<Message> history, SseEmitter emitter, String requestId);

}