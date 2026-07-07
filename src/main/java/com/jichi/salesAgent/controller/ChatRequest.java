package com.jichi.salesAgent.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "sessionId 不能为空")
        @Size(max = 100)
        String sessionId,

        @NotBlank(message = "message 不能为空")
        @Size(max = 2000, message = "消息不能超过 2000 字")
        String message
) {}
