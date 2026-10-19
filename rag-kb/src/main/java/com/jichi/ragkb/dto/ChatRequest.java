package com.jichi.ragkb.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String question;
    private List<Long> kbIds;
    private String sessionId;     // 为空时创建新会话
}