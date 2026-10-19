package com.jichi.ragkb.dto;

import lombok.Data;

@Data
public class KnowledgeBaseCreateRequest {
    private String name;
    private String description;
    private String departmentId;
    private Boolean isPublic = false;
}