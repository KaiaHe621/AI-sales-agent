package com.jichi.springai.controller.functionCall;

import com.jichi.springai.tools.NotificationTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notify")
public class NotificationController {

    private final ChatClient chatClient;
    private final NotificationTools notificationTools;

    public NotificationController(ChatClient.Builder builder, NotificationTools notificationTools) {
        this.notificationTools = notificationTools;
        this.chatClient = builder
                .defaultSystem("""
                        你是一个助手，可以帮用户发送邮件或创建日程提醒。
                        需要操作时直接调用工具，不要编造结果。
                        操作完成后用自然语言告知用户结果。
                        """)
                .build();
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .tools(notificationTools)
                .call()
                .content();
    }
}