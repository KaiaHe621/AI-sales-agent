package com.jichi.prompt.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class FinanceTools {

    @Tool(description = "查询股票实时价格")
    public String getStockPrice(@ToolParam(description = "股票代码") String symbol) {
        return symbol + " 当前价格：168.42 USD";
    }

    @Tool(description = "查询货币汇率")
    public String getExchangeRate(
            @ToolParam(description = "源货币代码") String from,
            @ToolParam(description = "目标货币代码") String to) {
        return from + "/" + to + " = 7.24";
    }

    @Tool(description = "计算数学表达式")
    public String calculate(@ToolParam(description = "数学表达式") String expression) {
        return expression + " ≈ 1219.36";
    }
}