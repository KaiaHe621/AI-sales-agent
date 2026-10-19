package com.jichi.agent.model;

import java.time.LocalDateTime;

/** 订单实体（Mock，实际项目换成 JPA @Entity） */
public class Order {

    private String id;
    private String userId;
    private double actualAmount;
    private String status;          // PAID / SHIPPED / COMPLETED / REFUNDED
    private LocalDateTime createdAt;
    private LocalDateTime signedAt; // 签收时间，null 表示尚未签收

    public Order(String id, String userId, double actualAmount,
                 String status, LocalDateTime createdAt, LocalDateTime signedAt) {
        this.id = id;
        this.userId = userId;
        this.actualAmount = actualAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.signedAt = signedAt;
    }

    public String getId()              { return id; }
    public String getUserId()          { return userId; }
    public double getActualAmount()    { return actualAmount; }
    public String getStatus()          { return status; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public LocalDateTime getSignedAt() { return signedAt; }
}