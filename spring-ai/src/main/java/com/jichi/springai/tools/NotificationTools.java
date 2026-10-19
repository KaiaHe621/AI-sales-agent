package com.jichi.springai.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationTools {

    private static final Logger log = LoggerFactory.getLogger(NotificationTools.class);

    private final JavaMailSender mailSender;
    // 简单的内存提醒存储，生产环境换成数据库
    private final Map<String, String> reminderStore = new ConcurrentHashMap<>();

    public NotificationTools(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * 无返回值工具：执行完模型就知道操作已完成
     */
    @Tool(description = "发送邮件通知给指定邮箱")
    public void sendEmail(
            @ToolParam(description = "收件人邮箱") String email,
            @ToolParam(description = "邮件主题") String subject,
            @ToolParam(description = "邮件正文") String body) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("邮件已发送至 {}", email);
        // void 返回，模型收到 tool result 后会自动继续生成回复
    }

    /**
     * 有返回值工具：返回提醒 ID，模型会把结果告诉用户
     */
    @Tool(description = "创建一个日程提醒，返回提醒ID")
    public String createReminder(
            @ToolParam(description = "提醒内容") String content,
            @ToolParam(description = "提醒时间，格式：yyyy-MM-dd HH:mm") String reminderTime) {

        String reminderId = "RMD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        reminderStore.put(reminderId, reminderTime + " | " + content);
        log.info("创建提醒 [{}]: {} at {}", reminderId, content, reminderTime);
        return "提醒已创建，ID: " + reminderId + "，将于 " + reminderTime + " 提醒你：" + content;
    }
}