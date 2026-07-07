package com.jichi.salesAgent.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sa_sales_order")
@Getter
@Setter
@NoArgsConstructor
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 50)
    private String orderNo;

    @Column(name = "rep_id", nullable = false)
    private Long repId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal profit;

    @Column(nullable = false, length = 20)
    private String status;   // COMPLETED / REFUNDED / CANCELLED

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
