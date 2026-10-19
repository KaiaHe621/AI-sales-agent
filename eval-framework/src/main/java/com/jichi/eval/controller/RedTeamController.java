package com.jichi.eval.controller;

import com.jichi.eval.security.RedTeamRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eval/security")
@RequiredArgsConstructor
public class RedTeamController {

    private final RedTeamRunner redTeamRunner;

    /**
     * 对指定 System Prompt 进行 Red Team 测试
     */
    @PostMapping("/red-team")
    public ResponseEntity<RedTeamRunner.RedTeamReport> runRedTeam(
            @RequestBody RedTeamRequest request
    ) {
        RedTeamRunner.RedTeamReport report = redTeamRunner.run(request.getSystemPrompt());

        if (report.isHasCriticalVulnerability()) {
            return ResponseEntity.status(422).body(report);
        }
        return ResponseEntity.ok(report);
    }

    @lombok.Data
    public static class RedTeamRequest {
        private String systemPrompt;
    }
}