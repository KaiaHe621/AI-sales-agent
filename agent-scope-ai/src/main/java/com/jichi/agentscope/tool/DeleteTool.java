package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

// 删除工具（仅管理员可用）
public class DeleteTool {
    @Tool(name = "delete_record", description = "删除指定业务记录，高风险操作")
    public String deleteRecord(
            @ToolParam(name = "record_id", description = "要删除的记录 ID") String recordId
    ) {
        return "记录 " + recordId + " 已删除";
    }
}