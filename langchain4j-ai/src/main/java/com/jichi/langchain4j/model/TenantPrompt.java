package com.jichi.langchain4j.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tenant_prompt")
@Data
public class TenantPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tenantId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}