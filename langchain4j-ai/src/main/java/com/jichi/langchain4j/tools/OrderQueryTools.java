package com.jichi.langchain4j.tools;

import com.jichi.langchain4j.model.DateRange;
import com.jichi.langchain4j.model.OrderStatus;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OrderQueryTools {

    // Mock 数据
    private static final Map<String, String[]> MOCK_ORDER_DETAIL = Map.of(
            "ORD20240101001", new String[]{"已发货", "2024-01-01 10:00", "2024-01-05", "顺丰 SF123456789"},
            "ORD20240101002", new String[]{"待付款", "2024-01-01 11:30", "-", "-"},
            "ORD20240101003", new String[]{"已完成", "2024-01-01 09:00", "2024-01-04", "圆通 YT987654321"}
    );

    // 基础类型
    @Tool("根据用户ID查询用户信息，返回用户名和注册时间")
    public String getUserById(@P("用户ID，正整数") long userId) {
        // Mock 数据
        return String.format("{\"id\":%d,\"username\":\"user%d\",\"createdAt\":\"2024-01-01\"}", userId, userId);
    }

    // String
    @Tool("根据用户名关键词模糊搜索用户，返回匹配的用户名列表")
    public List<String> searchUsers(@P("用户名关键词，至少2个字符") String keyword) {
        return List.of("user_" + keyword + "_01", "user_" + keyword + "_02");
    }

    // 枚举
    @Tool("查询指定状态的订单列表，返回订单ID和金额")
    public String getOrdersByStatus(
            @P("订单状态：PENDING待付款/PAID已付款/SHIPPED已发货/DELIVERED已完成/CANCELLED已取消")
            OrderStatus status) {
        return String.format("[{\"orderId\":\"ORD001\",\"status\":\"%s\",\"amount\":299.0}]", status);
    }

    // 复杂对象（模型会自动构造）
    @Tool("查询指定日期范围内的订单统计，返回订单数量和总金额")
    public String getOrderStats(
            @P("日期范围，包含 startDate 和 endDate，格式 YYYY-MM-DD")
            DateRange dateRange) {
        return String.format("{\"startDate\":\"%s\",\"endDate\":\"%s\",\"count\":42,\"totalAmount\":12800.0}",
                dateRange.startDate(), dateRange.endDate());
    }


    @Tool("根据订单号查询订单状态和物流信息")
    public String queryOrderStatus(
            @P("订单号，格式为 ORD 开头后接11位数字，如 ORD20240101001") String orderId) {

        // 参数校验
        if (orderId == null || !orderId.matches("ORD\\d{11}")) {
            return "错误：订单号格式不合法，请提供正确的订单号（ORD开头后接11位数字）";
        }

        try {
            String[] order = MOCK_ORDER_DETAIL.get(orderId);

            if (order == null) {
                return "未找到订单：" + orderId + "，请确认订单号是否正确";
            }

            return String.format(
                    "订单号：%s\n状态：%s\n下单时间：%s\n预计送达：%s\n物流：%s",
                    orderId, order[0], order[1], order[2], order[3]);

        } catch (Exception e) {
            log.error("查询订单失败：{}", orderId, e);
            return "查询订单时发生错误，请稍后重试或联系人工客服";
            // 只返回用户友好的错误信息，不暴露异常细节
        }
    }

}