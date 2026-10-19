package com.jichi.ragkb.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "kb_doc_chunk")
@Data
public class DocChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 向量字段，使用 PGVector 的 vector 类型。
     * Hibernate 6.4+ 原生支持 pgvector，通过 hibernate-vector 模块实现。
     * @JdbcTypeCode(SqlTypes.VECTOR) 告诉 Hibernate 这是向量类型。
     * @Array(length = 1024) 指定维度，对应 text-embedding-v3 的输出维度。
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 1024)
    @Column(name = "embedding", columnDefinition = "vector(1024)")
    private float[] embedding;

    @Column(name = "page_num")
    private Integer pageNum;

    @Column(name = "section_title", length = 500)
    private String sectionTitle;

    @Column(name = "token_count", nullable = false)
    private Integer tokenCount = 0;

    @Column(name = "doc_version", nullable = false)
    private Integer docVersion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}