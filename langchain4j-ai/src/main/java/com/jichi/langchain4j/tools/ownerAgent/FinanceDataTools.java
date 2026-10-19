package com.jichi.langchain4j.tools.ownerAgent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class FinanceDataTools {

    @Tool("获取指定股票的实时价格")
    public String getStockPrice(@P("股票代码，如 AAPL、600036") String symbol) {
        // 模拟数据，真实项目对接行情 API
        return symbol + " 当前价格：168.42 USD，涨跌：+1.23%";
    }

    @Tool("获取指定股票最近 N 天的价格走势，返回收盘价列表")
    public String getStockHistory(
            @P("股票代码") String symbol,
            @P("查询天数，1-90 之间") int days) {
        return symbol + " 近 " + days + " 天收盘价：[162.5, 164.3, 165.1, 166.8, 168.4]（模拟数据）";
    }

    @Tool("获取指定公司的核心财务指标：市盈率、营收、净利润")
    public String getFinancials(@P("公司名称或股票代码") String company) {
        return company + " 财务指标：市盈率 28.5，TTM 营收 3830 亿 USD，净利润 970 亿 USD";
    }

    @Tool("搜索关于指定主题的最新新闻摘要")
    public String searchNews(@P("搜索主题，建议带公司名或股票代码") String topic) {
        return topic + " 近期新闻：(1) 新品发布会获市场正面反应；(2) AI 布局持续加码；(3) 分析师维持买入评级（均为模拟数据）";
    }
}