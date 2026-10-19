package com.jichi.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class StockTools {

    @Tool("查询股票实时价格，返回股票代码、当前价格和涨跌幅")
    public String getStockPrice(@P("股票代码，例如 AAPL、TSLA、000001.SZ") String symbol) {
        return String.format("{\"symbol\":\"%s\",\"price\":168.42,\"change\":\"+1.2%%\"}", symbol);
    }
}