package com.jichi.langchain4j.controller.chatMemory;

import com.jichi.langchain4j.service.chatMemory.SessionManagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sessions")
public class SessionManagementController {

    private final SessionManagementService sessionService;

    public SessionManagementController(SessionManagementService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public List<SessionManagementService.SessionSummary> getSessions(
            @RequestParam String userId) {
        return sessionService.getUserSessions(userId);
    }

    @PostMapping("/new")
    public Map<String, String> newSession(@RequestParam String userId) {
        return Map.of("sessionId", sessionService.newSession(userId));
    }

    @DeleteMapping("/{sessionId}")
    public void deleteSession(@PathVariable String sessionId,
                               @RequestParam String userId) {
        sessionService.deleteSession(sessionId, userId);
    }
}