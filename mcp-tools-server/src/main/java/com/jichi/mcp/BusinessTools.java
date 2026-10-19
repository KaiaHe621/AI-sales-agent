package com.jichi.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class BusinessTools {

    @Tool(description = """
            查询指定日期范围内的销售汇总数据。
            返回：总销售额、订单量、客单价、环比变化。
            适用于：分析销售趋势、生成数据报告。
            """)
    public String querySales(
            @ToolParam(description = "开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(description = "结束日期，格式 yyyy-MM-dd") String endDate) {

        // 演示用模拟数据，实际接数据库
        return String.format("""
                %s 至 %s 销售数据：
                总销售额：¥128,450.00
                订单量：543 单
                客单价：¥236.56
                环比上期：+8.3%%
                """, startDate, endDate);
    }

    @Tool(description = """
            查询商品当前库存状态。
            返回：商品名称、当前库存量、库存状态（充足/预警/缺货）。
            """)
    public String queryInventory(
            @ToolParam(description = "商品名称关键词，支持模糊匹配") String keyword) {

        return String.format("""
                搜索「%s」的库存结果：
                无线耳机 Pro：库存 45 件（充足）
                无线耳机 Lite：库存 8 件（预警，安全线 20 件）
                有线耳机 X1：库存 0 件（缺货）
                """, keyword);
    }

    @Tool(description = "获取当前日期，以及本周、上周、本月的起止日期。用于其他工具需要日期参数时辅助计算。")
    public String getDateInfo() {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate lastMonday = monday.minusWeeks(1);

        return String.format("""
                今天：%s
                本周：%s 至 %s
                上周：%s 至 %s
                本月：%s 至今
                """,
                today,
                monday, today,
                lastMonday, monday.minusDays(1),
                today.withDayOfMonth(1));
    }
}