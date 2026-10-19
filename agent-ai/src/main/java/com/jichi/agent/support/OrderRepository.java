package com.jichi.agent.support;

import com.jichi.agent.model.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 订单 Mock 仓库：预置几条测试数据，覆盖各种校验分支 */
@Component
public class OrderRepository {

    private final Map<String, Order> store = new ConcurrentHashMap<>();

    public OrderRepository() {
        LocalDateTime now = LocalDateTime.now();
        // 正常订单（3 天前购买，已签收，可退款）
        store.put("ORD20240115001", new Order(
                "ORD20240115001", "user_001", 299.00,
                "COMPLETED", now.minusDays(3), now.minusDays(2)));
        // 已过期订单（10 天前购买）
        store.put("ORD20231201001", new Order(
                "ORD20231201001", "user_001", 599.00,
                "COMPLETED", now.minusDays(10), now.minusDays(9)));
        // 大额订单（用于触发人工审批，金额 > 1000）
        store.put("ORD20240116001", new Order(
                "ORD20240116001", "user_001", 1599.00,
                "COMPLETED", now.minusDays(1), now.minusHours(20)));
        // 配送中（尚未签收）
        store.put("ORD20240117001", new Order(
                "ORD20240117001", "user_001", 199.00,
                "SHIPPED", now.minusDays(1), null));
    }

    public Optional<Order> findById(String orderId) {
        return Optional.ofNullable(store.get(orderId));
    }
}