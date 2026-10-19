package com.jichi.voiceshopping.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "faq_entry")
public class FaqEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId = 0L;       // 0 = 平台通用

    @Column(nullable = false, length = 512)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false, length = 16)
    private String status = "PUBLISHED";

    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 0;

    // embedding 同样不映射，走 JdbcTemplate

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}