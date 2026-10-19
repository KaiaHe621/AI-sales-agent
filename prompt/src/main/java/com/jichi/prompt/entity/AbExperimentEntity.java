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
@Table(name = "ab_experiment")
public class AbExperimentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "experiment_id", nullable = false, unique = true, length = 100)
    private String experimentId;

    @Column(name = "prompt_key_a", nullable = false, length = 100)
    private String promptKeyA;

    @Column(name = "version_a", nullable = false, length = 20)
    private String versionA;

    @Column(name = "prompt_key_b", nullable = false, length = 100)
    private String promptKeyB;

    @Column(name = "version_b", nullable = false, length = 20)
    private String versionB;

    @Builder.Default
    @Column(name = "traffic_ratio_a", nullable = false)
    private int trafficRatioA = 50;

    // RUNNING / PAUSED / FINISHED
    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "RUNNING";

    @Setter(lombok.AccessLevel.NONE)
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}