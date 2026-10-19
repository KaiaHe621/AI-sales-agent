package com.jichi.langchain4j.controller.prompt;

import com.jichi.langchain4j.service.Assistant;
import com.jichi.langchain4j.service.CustomerServiceAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompt")
public class SystemMessageController {

    private final Assistant assistant;
    private final CustomerServiceAssistant csAssistant;

    public SystemMessageController(Assistant assistant,
                                   CustomerServiceAssistant csAssistant) {
        this.assistant = assistant;
        this.csAssistant = csAssistant;
    }

    @GetMapping("/tech")
    public String techChat(@RequestParam String question) {
        return assistant.chat(question);
    }

    @GetMapping("/cs")
    public String csChat(@RequestParam String message) {
        return csAssistant.chat(message);
    }
}