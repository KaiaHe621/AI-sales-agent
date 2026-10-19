package com.jichi.salesAgent.tool;

import com.jichi.salesAgent.entity.SalesOrder;
import com.jichi.salesAgent.service.SalesQueryService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SalesQueryTool {

    private final SalesQueryService queryService;

    @Tool("查询原始销售订单数据。适用于：查具体订单、看某时段订单列表、统计某时段订单总数。" +
         "【不适合】排名、增长率、图表生成、异常检测等场景，那些请使用对应的专用工具。")
    public String queryOrders(
            @P("查询开始日期，格式 yyyy-MM-dd，如 2024-11-01") String startDate,
            @P("查询结束日期，格式 yyyy-MM-dd，如 2024-11-30") String endDate,
            @P("大区名称，如：华东区、华南区、华北区、西南区。传 null 或空字符串表示查全公司") String regionName,
            @P("销售员姓名，如需按特定销售员筛选则传入，如：张磊。否则传 null 或空字符串") String repName,
            @P("最多返回条数，默认 20，最大 50。避免返回数据过多") int limit) {

        log.info("工具调用-queryOrders: start={}, end={}, region={}, repName={}, limit={}",
                startDate, endDate, regionName, repName, limit);

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // 大区名称转 ID
            Long regionId = null;
            if (regionName != null && !regionName.isBlank()) {
                regionId = queryService.getRegionIdByName(regionName);
                if (regionId == null) {
                    return "未找到大区：" + regionName + "，请确认大区名称是否正确（华东区/华南区/华北区/西南区）";
                }
            }

            // 销售员姓名转 ID
            Long repId = null;
            if (repName != null && !repName.isBlank()) {
                repId = queryService.getRepIdByName(repName);
                if (repId == null) {
                    return "未找到销售员：" + repName + "，请确认姓名是否正确";
                }
            }

            List<SalesOrder> orders = queryService.queryOrders(repId, regionId, start, end);

            if (orders.isEmpty()) {
                return String.format("在 %s 至 %s 期间，%s暂无订单数据",
                        startDate, endDate,
                        regionName != null ? regionName + " " : "");
            }

            // 按 limit 截断
            int actualLimit = Math.min(limit, 50);
            List<SalesOrder> limited = orders.size() > actualLimit
                    ? orders.subList(0, actualLimit) : orders;

            return formatOrders(limited, orders.size(), startDate, endDate, regionName);

        } catch (DateTimeParseException e) {
            return "日期格式错误，请使用 yyyy-MM-dd 格式，如：2024-11-01";
        } catch (Exception e) {
            log.error("查询订单失败", e);
            return "查询订单数据时出现问题，请稍后重试";
        }
    }

    private String formatOrders(List<SalesOrder> orders, int total,
                                  String startDate, String endDate, String regionName) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("订单查询结果（%s 至 %s%s）：\n",
                startDate, endDate,
                regionName != null ? "，" + regionName : ""));
        sb.append(String.format("共找到 %d 条订单", total));
        if (orders.size() < total) {
            sb.append(String.format("，以下显示前 %d 条", orders.size()));
        }
        sb.append("\n\n");

        for (SalesOrder order : orders) {
            String repName = queryService.getRepName(order.getRepId());
            sb.append(String.format("- 订单号：%s | 日期：%s | 销售员：%s | 客户：%s | 金额：¥%,.0f | 状态：%s\n",
                    order.getOrderNo(),
                    order.getOrderDate(),
                    repName,
                    order.getCustomerName(),
                    order.getAmount(),
                    translateStatus(order.getStatus())));
        }

        // 统计小计
        double completedTotal = orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()))
                .mapToDouble(o -> o.getAmount().doubleValue())
                .sum();
        long completedCount = orders.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus())).count();

        sb.append(String.format("\n小计：完成订单 %d 笔，金额合计 ¥%,.0f", completedCount, completedTotal));
        return sb.toString();
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "COMPLETED" -> "已完成";
            case "REFUNDED"  -> "已退款";
            case "CANCELLED" -> "已取消";
            default          -> status;
        };
    }
}