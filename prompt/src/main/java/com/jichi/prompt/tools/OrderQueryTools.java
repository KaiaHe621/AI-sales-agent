package com.jichi.prompt.tools;

import com.jichi.prompt.repository.OrderRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class OrderQueryTools {

    private final OrderRepository orderRepository;

    public OrderQueryTools(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Tool(description = "根据订单号查询订单状态和物流信息")
    public String queryOrderStatus(
            @ToolParam(description = "订单号，格式如 ORD20240101001") String orderId) {
        return orderRepository.findById(orderId)
                .map(order -> String.format(
                        "订单号：%s\n状态：%s\n下单时间：%s\n预计到达：%s\n物流信息：%s",
                        order.getId(), order.getStatus(),
                        order.getCreatedAt(), order.getExpectedDelivery(),
                        order.getLogisticsInfo()))
                .orElse("未找到订单，请确认订单号是否正确");
    }

    @Tool(description = "查询商品库存和价格")
    public String queryProduct(
            @ToolParam(description = "商品ID或商品名称") String productQuery) {
        return "商品：XX耳机，当前价格：299元，库存：有货，颜色：黑/白/红";
    }

    @Tool(description = "申请售后退换货")
    public String createAftersaleRequest(
            @ToolParam(description = "订单号") String orderId,
            @ToolParam(description = "售后类型：REFUND（退款）/EXCHANGE（换货）/REPAIR（维修）") String type,
            @ToolParam(description = "问题描述") String description) {
        String ticketId = "AS" + System.currentTimeMillis();
        return "售后申请已提交，工单号：" + ticketId +
               "\n预计24小时内处理，处理结果会通过短信通知您";
    }
}