package com.jichi.prompt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    private String id;              // 订单号，如 ORD20240101001

    private String status;          // 待发货/已发货/已签收/售后中

    private String logisticsInfo;   // 物流信息描述

    private LocalDateTime expectedDelivery;

    @CreationTimestamp
    @Setter(lombok.AccessLevel.NONE)
    private LocalDateTime createdAt;
}