package com.jichi.springai.entity;

public enum OrderStatus {
    PENDING("待处理"),
    SHIPPED("已发货"),
    DELIVERED("已签收"),
    CANCELLED("已取消");

    private final String displayName;

    OrderStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}