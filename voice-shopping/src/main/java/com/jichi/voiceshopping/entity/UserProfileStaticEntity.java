package com.jichi.voiceshopping.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "user_profile_static")
public class UserProfileStaticEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    private String gender;
    private Integer age;
    private String city;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Integer weightKg;

    @Column(name = "skin_type")
    private String skinType;

    @Column(name = "tech_savvy")
    private String techSavvy;

    @Column(name = "budget_band")
    private String budgetBand;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}