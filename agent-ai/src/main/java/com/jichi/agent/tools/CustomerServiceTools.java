package com.jichi.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class CustomerServiceTools {

    // Mock 订单数据，真实项目替换为 OrderRepository 查询
    private static final Map<String, String[]> MOCK_ORDERS = Map.of(
            "ORD20240115001", new String[]{"已发货", "SF1234567890", "广州转运中心", "明天前"},
            "ORD20240116002", new String[]{"待支付", null, null, null},
            "ORD20240117003", new String[]{"已完成", "YT9876543210", "已签收", "已到达"}
    );

    // Mock 商品数据，真实项目替换为 ProductRepository 查询
    private static final List<Map<String, Object>> MOCK_PRODUCTS = List.of(
            Map.of("name", "降噪无线耳机 Pro", "price", 599.0, "stock", 128, "rating", 4.8),
            Map.of("name", "入耳式有线耳机", "price", 99.0, "stock", 0, "rating", 4.2),
            Map.of("name", "骨传导运动耳机", "price", 399.0, "stock", 56, "rating", 4.5)
    );

    @Tool(description = """
            根据订单号查询订单状态和物流信息。
            适用于：用户询问订单发货情况、快递位置、预计到达时间等物流相关问题。
            返回：订单状态、物流单号、当前物流位置、预计到达时间。
            不适用于：查询订单商品详情或价格（用 getOrderDetail 工具）。
            """)
    public String getOrderTracking(
            @ToolParam(description = "订单号，格式 ORD + 数字，例如 ORD20240115001")
            String orderId) {

        String[] order = MOCK_ORDERS.get(orderId);
        if (order == null) return "未找到订单：" + orderId + "，请确认订单号是否正确";

        return String.format("""
                订单号：%s
                状态：%s
                物流单号：%s
                当前位置：%s
                预计到达：%s
                """,
                orderId, order[0],
                order[1] != null ? order[1] : "暂无（订单未发货）",
                order[2] != null ? order[2] : "暂无信息",
                order[3] != null ? order[3] : "待定");
    }

    @Tool(description = """
            根据关键词搜索商品，查看商品信息。
            适用于：用户咨询某类商品是否有货、价格区间、推荐哪款商品等。
            返回：匹配商品的名称、价格、库存数量、商品评分。
            """)
    public String searchProducts(
            @ToolParam(description = "搜索关键词，例如：耳机、无线、降噪") String keyword,
            @ToolParam(description = "最多返回几个结果，默认 3，最多 10") int limit) {

        List<Map<String, Object>> matched = MOCK_PRODUCTS.stream()
                .filter(p -> p.get("name").toString().contains(keyword))
                .limit(Math.min(limit, 10))
                .toList();

        if (matched.isEmpty()) return "没有找到与「" + keyword + "」相关的商品，可以试试其他关键词";

        StringBuilder sb = new StringBuilder("找到 " + matched.size() + " 款相关商品：\n");
        for (Map<String, Object> p : matched) {
            int stock = (int) p.get("stock");
            sb.append(String.format("- %s：¥%.2f，%s，评分 %.1f/5.0\n",
                    p.get("name"), (double) p.get("price"),
                    stock > 0 ? "有货（库存" + stock + "件）" : "暂时缺货",
                    (double) p.get("rating")));
        }
        return sb.toString();
    }
}