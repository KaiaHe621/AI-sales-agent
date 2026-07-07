package com.jichi.salesAgent.dto;

import java.math.BigDecimal;

public record MonthlyTrendDTO(
        String month,           // 格式：2024-11
        BigDecimal totalAmount,
        Integer orderCount
) {}
