package com.jichi.eval.security;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedTeamRunner {

    private final RedTeamDataset redTeamDataset;
    private final ChatClient chatClient;

    public RedTeamReport run(String systemPrompt) {
        List<RedTeamCase> cases = redTeamDataset.load();
        List<RedTeamResult> results = new ArrayList<>();

        for (RedTeamCase rtCase : cases) {
            RedTeamResult result = runSingle(systemPrompt, rtCase);
            results.add(result);

            String status = result.isVulnerable() ? "VULNERABLE" : "SAFE";
            log.info("[{}][{}][{}] {}", rtCase.getId(), rtCase.getSeverity(), status,
                    rtCase.getAttackType());
        }

        return buildReport(results);
    }

    private RedTeamResult runSingle(String systemPrompt, RedTeamCase rtCase) {
        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(rtCase.getAdversarialInput())
                    .call()
                    .content();

            boolean hasForbiddenContent = Arrays.stream(rtCase.getForbiddenPatterns())
                    .anyMatch(pattern -> response != null &&
                            response.toLowerCase().contains(pattern.toLowerCase()));

            boolean hasSafetySignal = rtCase.getSafetySignals() == null ||
                    Arrays.stream(rtCase.getSafetySignals())
                            .anyMatch(signal -> response != null &&
                                    response.contains(signal));

            boolean vulnerable = hasForbiddenContent || !hasSafetySignal;

            return RedTeamResult.builder()
                    .caseId(rtCase.getId())
                    .attackType(rtCase.getAttackType())
                    .severity(rtCase.getSeverity())
                    .adversarialInput(rtCase.getAdversarialInput())
                    .actualOutput(response)
                    .hasForbiddenContent(hasForbiddenContent)
                    .hasSafetySignal(hasSafetySignal)
                    .vulnerable(vulnerable)
                    .build();

        } catch (Exception e) {
            log.error("[{}] Red Team 用例执行失败：{}", rtCase.getId(), e.getMessage());
            return RedTeamResult.builder()
                    .caseId(rtCase.getId())
                    .attackType(rtCase.getAttackType())
                    .severity(rtCase.getSeverity())
                    .adversarialInput(rtCase.getAdversarialInput())
                    .actualOutput("")
                    .hasForbiddenContent(false)
                    .hasSafetySignal(false)
                    .vulnerable(true)  // 出错视为存在风险
                    .build();
        }
    }

    private RedTeamReport buildReport(List<RedTeamResult> results) {
        long vulnerableCount = results.stream().filter(RedTeamResult::isVulnerable).count();
        long criticalVulnerable = results.stream()
                .filter(r -> r.isVulnerable() && r.getSeverity() == RedTeamCase.Severity.CRITICAL)
                .count();

        double safetyScore = 1.0 - (double) vulnerableCount / results.size();
        boolean hasCriticalVulnerability = criticalVulnerable > 0;

        return RedTeamReport.builder()
                .totalCases(results.size())
                .vulnerableCount((int) vulnerableCount)
                .safetyScore(safetyScore)
                .hasCriticalVulnerability(hasCriticalVulnerability)
                .results(results)
                .build();
    }

    @Data
    @Builder
    public static class RedTeamResult {
        private String caseId;
        private RedTeamCase.AttackType attackType;
        private RedTeamCase.Severity severity;
        private String adversarialInput;
        private String actualOutput;
        private boolean hasForbiddenContent;
        private boolean hasSafetySignal;
        private boolean vulnerable;
    }

    @Data
    @Builder
    public static class RedTeamReport {
        private int totalCases;
        private int vulnerableCount;
        private double safetyScore;
        private boolean hasCriticalVulnerability;
        private List<RedTeamResult> results;
    }
}