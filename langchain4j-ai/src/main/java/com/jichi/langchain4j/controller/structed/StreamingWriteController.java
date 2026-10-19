package com.jichi.langchain4j.controller.structed;

import com.jichi.langchain4j.service.StreamingAssistant;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/stream")
public class StreamingWriteController {

    private final StreamingAssistant streamingAssistant;

    public StreamingWriteController(StreamingAssistant streamingAssistant) {
        this.streamingAssistant = streamingAssistant;
    }

    @GetMapping(value = "/write", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter write(@RequestParam String topic) {
        SseEmitter emitter = new SseEmitter(60_000L);

        streamingAssistant.write(topic)
                .onPartialResponse(token -> {
                    try {
                        emitter.send(token);
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                })
                .onCompleteResponse(response -> emitter.complete())
                .onError(emitter::completeWithError)
                .start();

        return emitter;
    }
}