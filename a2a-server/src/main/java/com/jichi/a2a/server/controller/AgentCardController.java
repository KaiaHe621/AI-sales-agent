package com.jichi.a2a.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class AgentCardController {

    /**
     * A2A 协议规定的固定路径，Client 通过这个接口发现 Agent 的能力
     * 必须实现，否则 Client 无法获取 AgentCard，调用直接失败
     */
    @GetMapping("/.well-known/agent.json")
    public Map<String, Object> agentCard() {
        return Map.of(
                "name", "销售数据分析 Agent",
                "description", "专门处理销售数据分析任务，能生成报表、识别异常、预测趋势",
                "url", "http://localhost:8080",
                "version", "1.0.0",
                "capabilities", Map.of(
                        "streaming", false,
                        "pushNotifications", false
                ),
                "defaultInputModes", List.of("text"),
                "defaultOutputModes", List.of("text", "data"),
                "skills", buildSkills()
        );
    }

    private List<Map<String, Object>> buildSkills() {
        return List.of(
                Map.of(
                        "id", "query-sales-summary",
                        "name", "查询销售汇总",
                        "description", "查询指定日期范围内的销售数据汇总，包括总销售额、订单数、客单价等核心指标",
                        "tags", List.of("sales", "query", "summary"),
                        "examples", List.of(
                                "查一下上周的销售情况",
                                "本月销售额是多少？和上月比怎么样？"
                        ),
                        "inputModes", List.of("text"),
                        "outputModes", List.of("text", "data")
                ),
                Map.of(
                        "id", "analyze-sales-anomaly",
                        "name", "销售异常检测",
                        "description", "分析指定时间段内的销售数据，识别异常波动，找出原因并给出处置建议",
                        "tags", List.of("sales", "analytics", "anomaly"),
                        "examples", List.of(
                                "昨天下午销售额突然跌了，帮我分析原因",
                                "最近7天有没有销售异常的商品？"
                        ),
                        "inputModes", List.of("text"),
                        "outputModes", List.of("text", "data")
                ),
                Map.of(
                        "id", "generate-sales-report",
                        "name", "生成销售报告",
                        "description", "根据用户提供的时间范围和地区生成销售分析报告，信息不全时会主动追问",
                        "tags", List.of("sales", "report"),
                        "examples", List.of("帮我生成一份销售报告", "生成上个月华东地区的销售报告"),
                        "inputModes", List.of("text"),
                        "outputModes", List.of("text", "data")
                )
        );
    }
}