package com.jichi.springai.tools;

import com.jichi.springai.entity.Order;
import com.jichi.springai.entity.Product;
import com.jichi.springai.repository.OrderRepository;
import com.jichi.springai.repository.ProductRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderQueryTools {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderQueryTools(OrderRepository orderRepository,
                           ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Tool(description = "根据订单号查询订单状态和物流信息")
    public String getOrderStatus(
            @ToolParam(description = "订单号，格式如：ORD001") String orderId) {

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return "未找到订单号为 " + orderId + " 的订单";
        }
        return String.format("""
                订单号：%s
                状态：%s
                金额：¥%.2f
                创建时间：%s
                预计到达：%s
                物流单号：%s
                """,
                order.getId(),
                order.getStatus().getDisplayName(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getEstimatedDelivery() != null ? order.getEstimatedDelivery() : "暂无",
                order.getTrackingNumber() != null ? order.getTrackingNumber() : "暂无");
    }

    @Tool(description = "查询用户的历史订单列表，返回最近的订单记录")
    public String getUserOrders(
            @ToolParam(description = "用户ID") Long userId,
            @ToolParam(description = "查询条数，默认5条，最多20条") int limit) {

        int safeLimit = Math.min(limit, 20);
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, safeLimit));

        if (orders.isEmpty()) {
            return "该用户暂无历史订单";
        }

        StringBuilder sb = new StringBuilder("最近 " + orders.size() + " 条订单：\n");
        for (Order order : orders) {
            sb.append(String.format("- %s (%s) ¥%.2f - %s\n",
                    order.getId(),
                    order.getCreatedAt().toLocalDate(),
                    order.getTotalAmount(),
                    order.getStatus().getDisplayName()));
        }
        return sb.toString();
    }

    @Tool(description = "搜索商品信息，根据关键词查找商品名称和库存")
    public String searchProducts(
            @ToolParam(description = "搜索关键词，如商品名称") String keyword,
            @ToolParam(description = "最大返回数量，默认5个") int maxResults) {

        List<Product> products = productRepository.searchByKeyword(
                keyword, PageRequest.of(0, Math.min(maxResults, 10)));

        if (products.isEmpty()) {
            return "没有找到与 \"" + keyword + "\" 相关的商品";
        }

        StringBuilder sb = new StringBuilder();
        for (Product product : products) {
            sb.append(String.format("- %s：¥%.2f，库存%d件，评分%.1f\n",
                    product.getName(),
                    product.getPrice(),
                    product.getStock(),
                    product.getRating()));
        }
        return sb.toString();
    }
}