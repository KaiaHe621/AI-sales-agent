package com.jichi.langchain4j.controller.tryCatch;

import com.jichi.langchain4j.service.tools.TraceAssistant;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/trace")
@Slf4j
public class TraceController {

    private final TraceAssistant traceAssistant;

    public TraceController(TraceAssistant traceAssistant) {
        this.traceAssistant = traceAssistant;
    }

    @GetMapping
    public String chat(@RequestHeader(value = "X-Session-Id", defaultValue = "default")
                       String sessionId,
                       @RequestParam String message) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        try {
            return traceAssistant.chat(sessionId, message);
        } finally {
            MDC.clear();
        }
    }
}