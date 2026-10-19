package com.jichi.voiceshopping.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "session")
public class SessionEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /** HOME_ENTRY / PRODUCT_PAGE / SEARCH_FALLBACK */
    @Column(nullable = false, length = 16)
    private String channel;

    @Column(name = "bound_product_id")
    private Long boundProductId;

    @Column(length = 16)
    private String locale;

    /** ORDERED / ABANDONED / FOLLOWUP / HANDOFF */
    @Column(length = 16)
    private String outcome;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens = 0;
}