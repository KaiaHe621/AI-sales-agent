package com.jichi.agentscope.tool;

import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class NotificationTool {

    private static final Set<String> VALID_USERS = Set.of("user-001", "user-002", "user-123");

    @Tool(name = "send_notification", description = "向指定用户发送系统内通知")
    public ToolResultBlock sendNotification(
            @ToolParam(name = "user_id", description = "目标用户 ID") String userId,
            @ToolParam(name = "message", description = "通知内容") String message
    ) {
        if (!VALID_USERS.contains(userId)) {
            return ToolResultBlock.error("用户 ID 不存在：" + userId);
        }
        System.out.printf("[通知发送] 用户=%s 内容=%s%n", userId, message);
        return ToolResultBlock.text("通知已发送给用户 " + userId);
    }
}