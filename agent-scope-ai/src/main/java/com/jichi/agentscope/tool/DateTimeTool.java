package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class DateTimeTool {

    @Tool(name = "get_current_datetime", description = "获取当前日期和时间")
    public String getCurrentDateTime(
            @ToolParam(name = "format", description = "日期格式，例如 yyyy-MM-dd HH:mm:ss") String format
    ) {
        String pattern = (format != null && !format.isBlank()) ? format : "yyyy-MM-dd HH:mm:ss";
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }
}