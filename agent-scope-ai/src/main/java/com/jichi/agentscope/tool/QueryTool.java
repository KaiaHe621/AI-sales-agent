package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

// 基础查询工具（普通用户可用）
public class QueryTool {
    @Tool(name = "query_record", description = "查询业务记录详情")
    public String queryRecord(
            @ToolParam(name = "record_id", description = "记录 ID") String recordId
    ) {
        return "记录 " + recordId + " 的详情：状态=正常，创建时间=2025-01-01";
    }
}