package com.jichi.agent.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 滚动摘要压缩器：消息超过阈值时，把旧消息压缩成一段摘要，
 * 摘要 + 最近 N 条消息继续往下跑，不丢信息
 */
@Component
public class RollingMemoryCompressor {

    private static final int COMPRESS_THRESHOLD = 20; // 超过 20 条时触发压缩
    private static final int KEEP_RECENT = 6;         // 压缩后保留最近 6 条原始消息

    private final ChatClient summaryClient;

    public RollingMemoryCompressor(@Qualifier("dashScopeChatModel") ChatModel chatModel) {
        // 专门用于摘要的轻量 ChatClient，不挂工具，职责单一
        this.summaryClient = ChatClient.builder(chatModel).build();
    }

    /**
     * 检查消息列表是否需要压缩，需要则执行，返回压缩后的列表
     */
    public List<Message> maybeCompress(List<Message> messages) {
        if (messages.size() < COMPRESS_THRESHOLD) {
            return messages;
        }

        // 把前面的旧消息压缩，保留最近几条
        List<Message> toCompress = messages.subList(0, messages.size() - KEEP_RECENT);
        List<Message> toKeep     = messages.subList(messages.size() - KEEP_RECENT, messages.size());

        String summary = summarize(toCompress);

        // 用摘要替换旧消息，放在消息历史的最前面
        List<Message> compressed = new ArrayList<>();
        compressed.add(new SystemMessage(
                "以下是此前对话的摘要（部分历史已被压缩）：\n" + summary));
        compressed.addAll(toKeep);

        return compressed;
    }

    private String summarize(List<Message> messages) {
        StringBuilder history = new StringBuilder();
        for (Message msg : messages) {
            String role = msg instanceof UserMessage ? "用户" :
                          msg instanceof AssistantMessage ? "助手" : "系统";
            history.append(role).append(": ").append(msg.getText()).append("\n");
        }

        return summaryClient.prompt()
                .system("""
                        请把以下对话历史压缩成一段简洁的摘要。
                        保留：任务目标、关键决策、重要数据、用户明确表达的偏好和约束。
                        丢弃：闲聊、重复内容、过程中的细节推理。
                        摘要用第三人称，限 200 字以内。
                        """)
                .user(history.toString())
                .call()
                .content();
    }
}