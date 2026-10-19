package com.jichi.voiceshopping.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Table(name = "user_profile_dynamic")
public class UserProfileDynamicEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Type(JsonBinaryType.class)
    @Column(name = "category_affinity", columnDefinition = "jsonb")
    private Map<String, Double> categoryAffinity;

    @Type(JsonBinaryType.class)
    @Column(name = "brand_affinity", columnDefinition = "jsonb")
    private Map<String, Double> brandAffinity;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "recent_viewed", columnDefinition = "bigint[]")
    private List<Long> recentViewed;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "recent_purchased", columnDefinition = "bigint[]")
    private List<Long> recentPurchased;

    @Column(name = "price_sensitivity")
    private BigDecimal priceSensitivity;

    @Column(name = "avg_order_amount")
    private BigDecimal avgOrderAmount;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}