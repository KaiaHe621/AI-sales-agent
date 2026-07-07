package com.jichi.salesAgent.dto;

public record AnomalyDTO(
        String type,          // 异常类型
        String severity,      // HIGH / MEDIUM / LOW
        String subject,       // 异常主体（大区名/产品名/销售员名）
        String description,   // 异常描述
        String suggestion     // 处理建议
) {}
