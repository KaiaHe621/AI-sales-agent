package com.jichi.agent.model;

public enum WorkflowState {
    START,
    INTENT_CLASSIFICATION,   // AI 节点：理解用户意图
    AUTH_CHECK,              // 确定性节点：权限校验
    CONDITION_CHECK,         // 确定性节点：业务规则判断（7 天期限等）
    AMOUNT_CALCULATION,      // 确定性节点：金额计算
    EXECUTION,               // 确定性节点：执行操作（扣款/退款）
    AI_GENERATION,           // AI 节点：生成回复内容
    NOTIFICATION,            // 确定性节点：发送通知
    HUMAN_REVIEW,            // 人工节点：大额或风险操作等待人工审批
    COMPLETED,
    FAILED
}