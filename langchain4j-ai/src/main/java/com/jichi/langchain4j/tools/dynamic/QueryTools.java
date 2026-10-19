package com.jichi.langchain4j.tools.dynamic;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 所有角色都有的只读工具
 */
@Component
public class QueryTools {

    private static final Map<String, String> ORDERS = Map.of(
            "ORD001", "订单ORD001：iPhone 15，已发货，预计明天到达",
            "ORD002", "订单ORD002：AirPods Pro，待发货，预计3天内发出",
            "ORD003", "订单ORD003：MacBook，已完成，评价已提交"
    );

    @Tool("根据订单号查询订单状态")
    public String queryOrder(@P("订单号，格式如 ORD001") String orderId) {
        return ORDERS.getOrDefault(orderId, "未找到订单：" + orderId);
    }

    @Tool("查询商品信息")
    public String queryProduct(@P("商品名称或关键词") String keyword) {
        if (keyword.contains("iPhone") || keyword.contains("手机")) {
            return "iPhone 15：5999元，现货，支持7天无理由退换";
        }
        if (keyword.contains("AirPods") || keyword.contains("耳机")) {
            return "AirPods Pro：1299元，现货，支持降噪";
        }
        return "暂无匹配商品：" + keyword;
    }
}