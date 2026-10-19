package com.jichi.ragkb.dto;

import lombok.Data;

@Data
public class DocumentUploadResponse {
    private Long docId;
    private String fileName;
    private String status;       // PENDING（已提交，等待索引）
    private String message;

    public static DocumentUploadResponse submitted(Long docId, String fileName) {
        DocumentUploadResponse r = new DocumentUploadResponse();
        r.setDocId(docId);
        r.setFileName(fileName);
        r.setStatus("PENDING");
        r.setMessage("文档已上传，正在后台索引，请通过 /status 接口查询进度");
        return r;
    }
}