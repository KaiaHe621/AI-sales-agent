package com.jichi.langchain4j.controller.aiservice;

import com.jichi.langchain4j.service.MultiCapabilityAssistant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dev")
public class DevAssistantController {

    private final MultiCapabilityAssistant assistant;

    public DevAssistantController(MultiCapabilityAssistant assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/review")
    public String reviewCode(@RequestBody String code) {
        return assistant.reviewCode(code);
    }

    @PostMapping("/doc")
    public String writeDoc(@RequestBody String techContent) {
        return assistant.writeDoc(techContent);
    }

    @PostMapping("/sql")
    public String optimizeSql(@RequestBody String sql) {
        return assistant.optimizeSql(sql);
    }
}