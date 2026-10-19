package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

// 批量更新工具（仅管理员可用）
public class BatchUpdateTool {
    @Tool(name = "batch_update", description = "批量更新记录状态")
    public String batchUpdate(
            @ToolParam(name = "ids",    description = "记录 ID 列表，逗号分隔") String ids,
            @ToolParam(name = "status", description = "目标状态") String status
    ) {
        return "已将记录 [" + ids + "] 批量更新为状态：" + status;
    }
}