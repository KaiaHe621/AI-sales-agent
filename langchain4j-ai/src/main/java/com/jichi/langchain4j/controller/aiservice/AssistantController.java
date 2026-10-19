package com.jichi.langchain4j.controller.aiservice;

import com.jichi.langchain4j.service.SimpleAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final SimpleAssistant assistant;

    public AssistantController(SimpleAssistant assistant) {
        this.assistant = assistant;
    }

    @GetMapping
    public String ask(@RequestParam String question) {
        return assistant.chat(question);
    }
}