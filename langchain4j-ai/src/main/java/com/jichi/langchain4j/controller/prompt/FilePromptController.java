package com.jichi.langchain4j.controller.prompt;

import com.jichi.langchain4j.service.FileBasedAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompt/file")
public class FilePromptController {

    private final FileBasedAssistant assistant;

    public FilePromptController(FileBasedAssistant assistant) {
        this.assistant = assistant;
    }

    @GetMapping
    public String chat(@RequestParam String company,
                       @RequestParam String scope,
                       @RequestParam String message) {
        return assistant.chat(company, scope, message);
    }
}