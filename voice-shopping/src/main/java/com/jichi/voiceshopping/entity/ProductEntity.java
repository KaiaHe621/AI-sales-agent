package com.jichi.voiceshopping.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "sku_code", nullable = false, unique = true, length = 64)
    private String skuCode;

    private String name;

    @Column(name = "category_l1", length = 32, nullable = false)
    private String categoryL1;

    @Column(name = "category_l2", length = 64, nullable = false)
    private String categoryL2;

    private String brand;

    private BigDecimal price;

    @Column(name = "original_price")
    private BigDecimal originalPrice;

    private Integer stock;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> attributes;

    private String description;

    @Column(name = "selling_points")
    private String sellingPoints;

    @Column(length = 16, nullable = false)
    private String status;

    @Column(name = "is_new_arrival")
    private Boolean isNewArrival;

    // 注意：embedding 字段故意不映射进 Entity
    // 避免 JPA 每次 SELECT 都把 4KB 向量加载进内存
    // 向量读写统一走 ProductVectorService (JdbcTemplate)

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}