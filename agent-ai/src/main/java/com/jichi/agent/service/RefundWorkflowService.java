package com.jichi.agent.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import com.jichi.agent.model.Order;
import com.jichi.agent.support.HumanInTheLoopService;
import com.jichi.agent.support.NotificationService;
import com.jichi.agent.support.OrderRepository;
import com.jichi.agent.support.RefundService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RefundWorkflowService {

    private final ChatClient aiClient;
    private final OrderRepository orderRepository;
    private final RefundService refundService;
    private final NotificationService notificationService;
    private final HumanInTheLoopService humanInTheLoopService;

    public RefundWorkflowService(@Qualifier("dashScopeChatModel") ChatModel chatModel,
                                 OrderRepository orderRepository,
                                 RefundService refundService,
                                 NotificationService notificationService,
                                 HumanInTheLoopService humanInTheLoopService) {
        // 这个 ChatClient 专门用于意图分类和内容生成，不挂任何工具
        // 职责单一：只做语言理解，不做任何业务操作
        this.aiClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个意图分类助手，只输出分类结果，不要解释。")
                .build();
        this.orderRepository = orderRepository;
        this.refundService = refundService;
        this.notificationService = notificationService;
        this.humanInTheLoopService = humanInTheLoopService;
    }

    public WorkflowResult processRefund(String userId, String orderId, String userMessage) {
        WorkflowContext ctx = new WorkflowContext(userId, orderId, userMessage);

        // ——— AI 节点：理解用户意图 ———
        // 放在最前面：后续的确定性逻辑需要知道用户想要什么（全退/部分退/换货）
        ctx.intent = classifyIntent(userMessage);

        // ——— 确定性节点：权限和订单校验 ———
        // 这两步绝对不能交给 AI：AI 可能被"我真的很需要这笔钱"这种描述影响判断
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return WorkflowResult.fail("订单不存在：" + orderId);
        }
        if (!order.getUserId().equals(userId)) {
            return WorkflowResult.fail("无权操作此订单");
        }

        // ——— 确定性节点：业务规则判断 ———
        // 7 天期限、订单状态校验——规则明确，结果唯一，必须代码来判断
        RefundEligibility eligibility = checkEligibility(order);
        if (!eligibility.eligible()) {
            // 不符合条件时，用 AI 生成一段友善的拒绝回复
            // 注意：是 AI 生成"怎么说"，而不是 AI 决定"能不能退"
            String reply = generateRejectReply(userMessage, eligibility.reason());
            return WorkflowResult.reject(reply);
        }

        // ——— 确定性节点：金额计算 ———
        // 金额计算绝对不能交给 AI，小数点问题、四舍五入规则，容不得模糊
        double refundAmount = calculateRefundAmount(order, ctx.intent);

        // ——— 人工审批节点：大额退款需要主管确认 ———
        // 规则明确（> 1000 元），触发条件是确定性的；但审批决定权交给人类
        if (refundAmount > 1000) {
            String workflowId = UUID.randomUUID().toString();
            humanInTheLoopService.requestApproval(
                    workflowId,
                    "大额退款申请：¥" + String.format("%.2f", refundAmount) + "，订单：" + orderId,
                    "supervisor@company.com",
                    Map.of("orderId", orderId, "userId", userId, "amount", refundAmount)
            );
            return WorkflowResult.pending("退款申请已提交，金额较大需人工审核，预计 1 小时内处理");
        }

        // ——— 确定性节点：执行退款（≤ 1000 元直接处理）———
        refundService.process(orderId, refundAmount);

        // ——— AI 节点：生成用户友好的确认回复 ———
        String reply = generateApprovalReply(order, refundAmount);

        // ——— 确定性节点：发送通知 ———
        notificationService.sendRefundNotification(userId, orderId, refundAmount);

        return WorkflowResult.success(reply, refundAmount);
    }

    // AI 节点：意图分类
    // 强制输出格式，防止 AI 自由发挥输出无法解析的内容
    private String classifyIntent(String message) {
        return aiClient.prompt()
                .user("""
                        分类用户意图，只输出以下之一（不要有其他文字）：
                        FULL_REFUND（全额退款）
                        PARTIAL_REFUND（部分退款）
                        EXCHANGE（换货）
                        COMPLAINT（投诉，不需要退款）
                        
                        用户说：""" + message)
                .call()
                .content()
                .strip();
    }

    // 确定性节点：退款资格校验
    private RefundEligibility checkEligibility(Order order) {
        long daysSincePurchase = ChronoUnit.DAYS.between(
                order.getCreatedAt().toLocalDate(), LocalDate.now());

        if (daysSincePurchase > 7) {
            return RefundEligibility.deny(
                    "已超过 7 天无理由退货期限（购买于 " + daysSincePurchase + " 天前）");
        }

        if ("SHIPPED".equals(order.getStatus()) && order.getSignedAt() == null) {
            return RefundEligibility.deny("商品正在配送中，请收货后申请退款");
        }

        if ("REFUNDED".equals(order.getStatus())) {
            return RefundEligibility.deny("订单已退款，不可重复申请");
        }

        return RefundEligibility.pass();
    }

    // 确定性节点：退款金额计算
    private double calculateRefundAmount(Order order, String intent) {
        return switch (intent) {
            case "FULL_REFUND", "EXCHANGE" -> order.getActualAmount();
            case "PARTIAL_REFUND" -> order.getActualAmount() * 0.5;
            default -> order.getActualAmount();
        };
    }

    // AI 节点：生成拒绝回复
    // AI 只负责"怎么说"，不决定"能不能退"
    private String generateRejectReply(String userRequest, String reason) {
        return aiClient.prompt()
                .system("你是一个专业、友善的客服，用温和的语气回复用户，表示理解但无法处理")
                .user("用户申请退款，但不符合条件。\n用户说：" + userRequest + "\n不符合原因：" + reason + "\n请生成回复")
                .call()
                .content();
    }

    // AI 节点：生成通过回复
    private String generateApprovalReply(Order order, double refundAmount) {
        return aiClient.prompt()
                .system("你是一个专业、友善的客服")
                .user(String.format("退款已通过。订单 %s，退款金额 ¥%.2f，请生成简短的确认回复，让用户知道款项会在 3-5 个工作日内到账",
                        order.getId(), refundAmount))
                .call()
                .content();
    }

    static class WorkflowContext {
        final String userId;
        final String orderId;
        final String userMessage;
        String intent = "";

        WorkflowContext(String userId, String orderId, String userMessage) {
            this.userId = userId;
            this.orderId = orderId;
            this.userMessage = userMessage;
        }
    }

    record RefundEligibility(boolean eligible, String reason) {
        static RefundEligibility pass() {
            return new RefundEligibility(true, "");
        }

        static RefundEligibility deny(String reason) {
            return new RefundEligibility(false, reason);
        }
    }

    public record WorkflowResult(String status, String message, Double refundAmount) {
        static WorkflowResult success(String msg, double amount) {
            return new WorkflowResult("SUCCESS", msg, amount);
        }

        static WorkflowResult reject(String msg) {
            return new WorkflowResult("REJECTED", msg, null);
        }

        static WorkflowResult fail(String msg) {
            return new WorkflowResult("FAILED", msg, null);
        }

        static WorkflowResult pending(String msg) {
            return new WorkflowResult("PENDING", msg, null);
        }
    }
}