package com.jichi.agent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AssistantTools {

    @Tool(description = """
            查询指定城市的实时天气。
            适用于：用户询问某城市当前天气、今天天气、是否需要带伞等问题。
            返回：城市、天气状况、温度范围、湿度、风力。
            注意：只查当前天气，不查天气预报。
            """)
    public String getWeather(
            @ToolParam(description = "城市名，中文，例如：北京、上海、深圳") String city) {
        // 演示用模拟数据，真实项目对接天气 API（高德、心知天气等）
        return String.format("""
                城市：%s
                天气：晴转多云
                温度：12~20°C
                湿度：55%%
                风力：东南风 2 级
                查询时间：%s
                """, city, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    @Tool(description = "获取当前日期和时间。适用于用户询问今天几号、现在几点、这周星期几等时间相关问题。")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("""
                当前日期：%s
                当前时间：%s
                星期：%s
                """,
                now.toLocalDate(),
                now.format(DateTimeFormatter.ofPattern("HH:mm")),
                getDayOfWeekChinese(now));
    }

    @Tool(description = """
            查询两种货币之间的汇率。
            适用于：用户询问人民币兑换外币的比率、外汇换算、出国换多少钱等问题。
            返回：汇率数值和换算说明。
            """)
    public String getExchangeRate(
            @ToolParam(description = "源货币，3位代码，例如：CNY、USD、EUR、JPY") String from,
            @ToolParam(description = "目标货币，3位代码") String to) {
        // 演示用模拟汇率，真实项目对接汇率 API
        if ("CNY".equals(from) && "USD".equals(to)) {
            return "1 CNY = 0.138 USD（1 美元 ≈ 7.24 人民币）";
        }
        if ("USD".equals(from) && "CNY".equals(to)) {
            return "1 USD = 7.24 CNY";
        }
        return from + " 兑 " + to + " 的汇率：暂无实时数据，建议查询银行官网";
    }

    @Tool(description = """
            创建一个定时提醒事项。
            适用于：用户说"提醒我…"、"帮我记一下…"、"x点提醒我"等需要设置提醒的场景。
            返回：提醒创建结果（成功或失败）。
            """)
    public String createReminder(
            @ToolParam(description = "提醒内容描述") String content,
            @ToolParam(description = "提醒时间，格式：yyyy-MM-dd HH:mm，例如：2024-03-20 09:00") String remindAt) {
        // 真实项目里这里存数据库并触发推送通知（微信模板消息、短信等）
        return String.format("提醒已创建：「%s」，将在 %s 提醒你", content, remindAt);
    }

    private String getDayOfWeekChinese(LocalDateTime dt) {
        return switch (dt.getDayOfWeek()) {
            case MONDAY    -> "星期一";
            case TUESDAY   -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY  -> "星期四";
            case FRIDAY    -> "星期五";
            case SATURDAY  -> "星期六";
            case SUNDAY    -> "星期日";
        };
    }
}