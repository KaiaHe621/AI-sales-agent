package com.jichi.agent.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 通知服务 Mock */
@Component
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void sendRefundNotification(String userId, String orderId, double amount) {
        log.info("[通知] 用户 {} 订单 {} 退款 ¥{} 成功，短信/邮件已发送",
                userId, orderId, String.format("%.2f", amount));
    }

    public void notifyApprover(String approverEmail, String message) {
        log.info("[审批通知] 发送给 {}：{}", approverEmail, message);
    }
}