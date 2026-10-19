package com.jichi.eval.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EvalResult {

    /** 对应的用例 ID */
    private String caseId;

    /** AI 系统的实际输出 */
    private String actualOutput;

    /** 评估得分（0.0 ~ 1.0） */
    private double score;

    /** 评估维度名称 */
    private String dimension;

    /** 评估说明（规则触发原因 or Judge 理由） */
    private String reason;

    /** 是否通过（score >= threshold） */
    private boolean passed;
}