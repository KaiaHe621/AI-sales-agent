package com.jichi.langchain4j.controller.context;

import com.jichi.langchain4j.service.context.CustomerAgent;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dynamic/context")
public class ContextAgentController {

    private final CustomerAgent customerAgent;

    public ContextAgentController(CustomerAgent customerAgent) {
        this.customerAgent = customerAgent;
    }

    @GetMapping
    public String chat(@RequestHeader("X-User-Id") String userId,
                       @RequestParam String message) {
        // userId 已经通过 Header 注入到 UserContextTools，这里不需要再传
        return customerAgent.chat(message);
    }
}