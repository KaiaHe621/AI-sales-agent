package com.jichi.salesAgent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OrderSummaryDTO(
        String orderNo,
        String repName,
        String customerName,
        BigDecimal amount,
        String status,
        LocalDate orderDate
) {}
