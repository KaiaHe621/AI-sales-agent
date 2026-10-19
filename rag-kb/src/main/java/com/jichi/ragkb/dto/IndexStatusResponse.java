package com.jichi.ragkb.dto;

import lombok.Data;

@Data
public class IndexStatusResponse {
    private Long docId;
    private String fileName;
    private String status;         // PENDING / PROCESSING / DONE / FAILED
    private String errorMsg;
    private Integer chunkCount;
    private Integer tokenCount;
    private String indexedAt;
    private Integer retryCount;
}