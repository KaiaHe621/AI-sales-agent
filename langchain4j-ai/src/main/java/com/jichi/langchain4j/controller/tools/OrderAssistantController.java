package com.jichi.langchain4j.controller.tools;

import com.jichi.langchain4j.service.tools.OrderAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tool/order")
public class OrderAssistantController {

    private final OrderAssistant orderAssistant;

    public OrderAssistantController(OrderAssistant orderAssistant) {
        this.orderAssistant = orderAssistant;
    }

    @GetMapping
    public String chat(@RequestParam String message,
                       @RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId) {
        return orderAssistant.chat(sessionId, message);
    }
}