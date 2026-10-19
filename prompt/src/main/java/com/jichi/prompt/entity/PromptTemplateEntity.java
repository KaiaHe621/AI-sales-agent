package com.jichi.prompt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "prompt_template",
    uniqueConstraints = @UniqueConstraint(columnNames = {"template_key", "version", "environment"})
)
public class PromptTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false, length = 100)
    private String templateKey;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String description;

    // DRAFT / ACTIVE / ARCHIVED
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    // production / staging / dev
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String environment = "production";

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Setter(lombok.AccessLevel.NONE)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}