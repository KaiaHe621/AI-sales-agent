package com.jichi.voiceshopping.controller;

import com.jichi.voiceshopping.agent.AgentFactory;
import com.jichi.voiceshopping.dto.EmotionResult;
import com.jichi.voiceshopping.entity.StreamChunk;
import com.jichi.voiceshopping.service.LongTermMemoryWriter;
import com.jichi.voiceshopping.service.OrchestratorService;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;
import com.jichi.voiceshopping.voice.AsrService;
import com.jichi.voiceshopping.voice.TtsService;
import io.reactivex.Flowable;
import io.reactivex.processors.PublishProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceWebSocketHandler extends AbstractWebSocketHandler {

    private final AsrService asr;
    private final TtsService tts;
    private final ObjectMapper mapper;
    private final OrchestratorService orchestratorService;
    private final LongTermMemoryWriter longTermMemoryWriter;
    private final AgentFactory agentFactory;

    // 每个 WebSocket session 有自己的音频流
    private final Map<String, PublishProcessor<ByteBuffer>> audioStreams = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        PublishProcessor<ByteBuffer> audioStream = PublishProcessor.create();
        audioStreams.put(session.getId(), audioStream);

        asr.recognize(audioStream, (text, isEnd) -> {
            // session 可能已经被前端关了（ASR 服务端的尾部结果回来晚于 close），直接跳过
            if (!session.isOpen()) return;
            try {
                session.sendMessage(new TextMessage(mapper.writeValueAsString(
                        Map.of("type", "asr", "text", text, "final", isEnd))));
                if (isEnd) {
                    // 这里后面会接 Orchestrator：给 Orchestrator 塞 text
                    onUserUtterance(session, text);
                }
            } catch (Exception e) {
                log.warn("下发 ASR 结果失败（session 可能已关闭）: {}", e.getMessage());
            }
        });
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        PublishProcessor<ByteBuffer> stream = audioStreams.get(session.getId());
        if (stream != null) stream.onNext(message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        PublishProcessor<ByteBuffer> stream = audioStreams.remove(session.getId());
        if (stream != null) stream.onComplete();
        String sessionId = (String) session.getAttributes().get("sessionId");
        Long userId = (Long) session.getAttributes().get("userId");
        if (sessionId != null && userId != null) {
            longTermMemoryWriter.flushOnSessionEnd(sessionId, userId);
            agentFactory.remove(sessionId);
        }
    }

    /**
     * 用户一句话识别完，触发 Agent 流程（这节先只做回显）
     */
//    private void onUserUtterance(WebSocketSession session, String utterance) {
//        // ASR 断句触发时可能 text 是空的（纯静音），跳过回显避免空包惹 TTS 报错
//        if (utterance == null || utterance.isBlank()) return;
//
//        Flowable<String> textFlow = Flowable.just("鸡哥听到了，你说的是：" + utterance);
//        tts.synthesize(textFlow).subscribe(
//                audio -> {
//                    try {
//                        if (session.isOpen()) session.sendMessage(new BinaryMessage(audio));
//                    } catch (Exception e) {
//                        log.error("下发 TTS 失败", e);
//                    }
//                },
//                err -> log.error("TTS 订阅出错", err),    // 不接 onError 会把异常抛到 OkHttp 线程
//                () -> log.debug("TTS 下行完成 session={}", session.getId())
//        );
//    }
    private void onUserUtterance(WebSocketSession session, String utterance) {
        if (utterance == null || utterance.isBlank()) return;

        String sessionId = (String) session.getAttributes().get("sessionId");
        Long userId = (Long) session.getAttributes().get("userId");

        try {
            Flux<StreamChunk> chunks = orchestratorService.streamHandle(sessionId, userId, utterance);

            chunks.subscribe(chunk -> {
                if (!session.isOpen()) return;            // session 关掉之后不再写
                try {
                    switch (chunk.type()) {
                        case TEXT -> sendJson(session,
                                Map.of("type", "caption", "text", chunk.text()));
                        case AUDIO -> session.sendMessage(new BinaryMessage(chunk.audio()));
                        case PRODUCTS -> sendJson(session,
                                Map.of("type", "recommendation", "items", chunk.products()));
                    }
                } catch (IOException e) {
                    log.warn("WebSocket 发送失败（session 可能已关闭）: {}", e.getMessage());
                }
            }, err -> log.error("流式失败 sessionId={}", sessionId, err));
        } catch (Exception e) {
            log.error("流式处理错误 sessionId={}", sessionId, e);
        }
    }

    /**
     * 序列化任意对象后通过 WebSocket 文本帧发给前端
     */
    private void sendJson(WebSocketSession session, Object payload) throws IOException {
        session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
    }

    //    private void onUserUtterance(WebSocketSession session, String utterance) {
//        String sessionId = resolveSessionId(session);
//        Long userId = resolveUserId(session);
//        try {
//            EmotionResult result = orchestratorService.handle(sessionId, userId, utterance);
//
//            // 把 displayBlocks 作为 JSON 事件下发
//            session.sendMessage(new TextMessage(mapper.writeValueAsString(Map.of(
//                    "type", "recommendation",
//                    "items", result.displayBlocks()
//            ))));
//
//            // 把 speechText 流式合成
//            Flowable<String> textFlow = splitIntoSentences(result.speechText());
//            tts.synthesize(textFlow).subscribe(audio -> {
//                try {
//                    session.sendMessage(new BinaryMessage(audio));
//                } catch (Exception e) {
//                    log.error("下发 TTS 失败", e);
//                }
//            });
//        } catch (Exception e) {
//            log.error("Orchestrator 处理失败", e);
//            // 降级回复
//            tts.synthesize(Flowable.just("鸡哥这边网络有点卡，你再说一次？"))
//                    .subscribe(audio -> sendSafely(session, audio));
//        }
//    }
    private Flowable<String> splitIntoSentences(String text) {
        // 按句号、问号分段送 TTS，实现"边生成边合成"
        String[] parts = text.split("(?<=[。！？])");
        return Flowable.fromArray(parts);
    }

    private String resolveSessionId(WebSocketSession session) {
        Object v = session.getAttributes().get("sessionId");
        return v == null ? session.getId() : v.toString();
    }

    private Long resolveUserId(WebSocketSession session) {
        Object v = session.getAttributes().get("userId");
        return v == null ? 0L : (v instanceof Long l ? l : Long.parseLong(v.toString()));
    }

    private void sendSafely(WebSocketSession session, ByteBuffer audio) {
        try {
            session.sendMessage(new BinaryMessage(audio));
        } catch (Exception e) {
            log.warn("降级音频下发失败 sessionId={}", session.getId(), e);
        }
    }
}