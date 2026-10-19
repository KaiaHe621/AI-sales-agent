package com.jichi.ragkb.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "kb_eval_result")
@Data
public class EvalResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Column(nullable = false, length = 50)
    private String evalVersion;

    @Column(nullable = false)
    private Boolean hit;

    private Integer rank;

    @Column(columnDefinition = "TEXT")
    private String actualAnswer;

    private Double faithfulness;

    private Double answerRelevancy;

    @Column(nullable = false)
    private LocalDateTime evalAt = LocalDateTime.now();
}