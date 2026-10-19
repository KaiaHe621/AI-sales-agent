package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class SalesQueryTool {

    @Tool(name = "query_sales", description = "查询销售数据，支持按区域和日期范围过滤")
    public String querySales(
            @ToolParam(name = "region",     description = "查询区域，如：华东、华南，不传则查全国", required = false) String region,
            @ToolParam(name = "start_date", description = "开始日期，格式 yyyy-MM-dd", required = false) String startDate,
            @ToolParam(name = "end_date",   description = "结束日期，格式 yyyy-MM-dd", required = false) String endDate
    ) {
        String regionLabel = (region != null && !region.isBlank()) ? region : "全国";
        // Mock 数据，实际项目替换为数据库查询
        return String.format(
                "区域：%s，时间：%s ~ %s，销售额：¥1,250,000，订单量：3,420 笔，同比增长：+12.3%%",
                regionLabel,
                startDate != null ? startDate : "本月初",
                endDate   != null ? endDate   : "今日"
        );
    }
}