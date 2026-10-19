package com.jichi.prompt.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class StockTools {

    @Tool(description = "查询股票实时价格")
    public String getStockPrice(
            @ToolParam(description = "股票代码，例如：AAPL、600036") String symbol) {
        return String.format("股票代码：%s，当前价格：168.42 USD", symbol);
    }
}