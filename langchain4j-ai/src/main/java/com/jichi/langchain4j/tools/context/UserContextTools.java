package com.jichi.langchain4j.tools.context;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

// 不加 @Component：避免被 Spring 全局组件扫描收录，
// 防止其他 @AiService 在启动时把这个 @RequestScope Bean 注入进去报错。
// 由 CustomerAgentConfig 通过 @Bean @RequestScope 显式注册。
@Slf4j
public class UserContextTools {

    private final String currentUserId;

    // Mock 订单数据：userId → 订单列表
    private static final Map<String, String> USER_ORDERS = Map.of(
            "user-001", "订单ORD001（iPhone 15，已发货）、订单ORD004（键盘，待付款）",
            "user-002", "订单ORD002（AirPods，待发货）",
            "admin-001", "订单ORD003（MacBook，已完成）"
    );

    // Mock 订单归属：orderId → userId
    private static final Map<String, String> ORDER_OWNER = Map.of(
            "ORD001", "user-001",
            "ORD002", "user-002",
            "ORD003", "admin-001",
            "ORD004", "user-001"
    );

    public UserContextTools(HttpServletRequest request) {
        // 从请求头读，不从模型参数读——模型碰不到这个值
        this.currentUserId = request.getHeader("X-User-Id");
        log.info("[UserContextTools] 当前用户：{}", currentUserId);
    }

    @Tool("查询当前登录用户的订单列表，不需要提供用户ID")
    public String getMyOrders() {
        if (currentUserId == null || currentUserId.isBlank()) {
            return "未登录，无法查询订单";
        }
        return USER_ORDERS.getOrDefault(currentUserId, "暂无订单记录");
    }

    @Tool("为当前用户申请订单退款，会自动校验订单是否属于当前用户")
    public String applyRefund(
            @P("订单号") String orderId,
            @P("退款原因") String reason) {

        // 二次校验：即使模型传了别人的订单号，也会在这里拦住
        String owner = ORDER_OWNER.get(orderId);
        if (owner == null) {
            return "订单不存在：" + orderId;
        }
        if (!owner.equals(currentUserId)) {
            return "错误：无权操作此订单（订单不属于当前用户）";
        }

        return String.format("退款申请已提交：订单%s，原因：%s，预计1-3工作日退款", orderId, reason);
    }
}