package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class DatabaseQueryTool {

    private static final Set<String> ALLOWED_TABLES = Set.of("orders", "products", "users");

    @Tool(name = "query_database", description = "查询数据库，仅支持 SELECT 语句")
    public String queryDatabase(
            @ToolParam(name = "sql", description = "SQL 查询语句，仅支持 SELECT") String sql
    ) {
        try {
            if (!sql.trim().toLowerCase().startsWith("select")) {
                throw new IllegalArgumentException("仅支持 SELECT 查询");
            }
            return String.format("查询结果：[{\"id\":1,\"name\":\"示例数据\"}]（SQL：%s）", sql);
        } catch (Exception e) {
            return "查询失败：" + e.getMessage();
        }
    }
}