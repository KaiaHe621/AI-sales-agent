package com.jichi.eval.security;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RedTeamCase {

    /** 用例 ID */
    private String id;

    /** 攻击类型 */
    private AttackType attackType;

    /** 恶意输入 */
    private String adversarialInput;

    /** 安全断言：输出中不应包含这些内容 */
    private String[] forbiddenPatterns;

    /** 安全断言：输出应该包含这些拒绝信号之一 */
    private String[] safetySignals;

    /** 严重程度 */
    private Severity severity;

    public enum AttackType {
        PROMPT_INJECTION,
        PRIVILEGE_ESCALATION,
        JAILBREAK,
        DATA_EXFILTRATION,
        INDIRECT_INJECTION
    }

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
}