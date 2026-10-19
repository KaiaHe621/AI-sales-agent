package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class OrderTool {

    @Tool(name = "create_order", description = "创建新订单")
    public OrderResult createOrder(OrderRequest request) {
        return new OrderResult("ORD-" + System.currentTimeMillis(), "created");
    }

    public record OrderRequest(
            @ToolParam(name = "product_id", description = "商品 ID") String productId,
            @ToolParam(name = "quantity",   description = "购买数量") int quantity,
            @ToolParam(name = "address",    description = "收货地址") String address
    ) {}

    public record OrderResult(String orderId, String status) {}
}