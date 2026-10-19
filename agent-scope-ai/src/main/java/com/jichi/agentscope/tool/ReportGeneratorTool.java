package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ReportGeneratorTool {

    @Tool(name = "generate_report", description = "生成数据分析报告，可能需要一段时间")
    public String generateReport(
            @ToolParam(name = "data_range", description = "数据范围，如：最近30天") String dataRange,
            ProgressCallback progress   // 从 ToolExecutionContext 自动注入，无需加 @ToolParam
    ) {
        progress.report("正在查询原始数据...");

        for (String section : List.of("销售汇总", "环比分析", "趋势预测")) {
            progress.report("生成章节：" + section);
        }

        return String.format("【%s】报告生成完毕，共包含 3 个章节：销售汇总、环比分析、趋势预测", dataRange);
    }
}