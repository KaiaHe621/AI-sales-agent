package com.jichi.langchain4j.controller.scene;

import com.jichi.langchain4j.service.scene.SceneAwareAgentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dynamic/scene")
public class SceneAgentController {

    private final SceneAwareAgentService agentService;

    public SceneAgentController(SceneAwareAgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public String chat(@RequestHeader(value = "X-Session-Id", defaultValue = "default") String sessionId,
                       @RequestParam(defaultValue = "pre_sale") String scene,
                       @RequestParam String message) {
        return agentService.chat(sessionId, scene, message);
    }
}