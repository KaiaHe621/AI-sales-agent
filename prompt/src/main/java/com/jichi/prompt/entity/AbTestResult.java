package com.jichi.prompt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ab_test_result")
public class AbTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_id", nullable = false, length = 100)
    private String experimentId;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    // A 或 B
    @Column(nullable = false, length = 5)
    private String variant;

    @Column(nullable = false)
    private boolean success;

    // 用户反馈评分，1-5，没反馈时为 null
    @Column(name = "user_rating")
    private Integer userRating;

    @Setter(lombok.AccessLevel.NONE)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // PromptAbTestService 里直接 new，提供一个便捷构造
    public AbTestResult(String experimentId, String userId, String variant,
                        boolean success, Integer userRating, LocalDateTime createdAt) {
        this.experimentId = experimentId;
        this.userId = userId;
        this.variant = variant;
        this.success = success;
        this.userRating = userRating;
        this.createdAt = createdAt;
    }
}