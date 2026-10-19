package com.jichi.langchain4j.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message")
@Data
@NoArgsConstructor
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String role;        // SYSTEM / USER / AI / TOOL

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String toolName;    // 工具调用消息时的工具名

    @CreationTimestamp
    private LocalDateTime createdAt;
}