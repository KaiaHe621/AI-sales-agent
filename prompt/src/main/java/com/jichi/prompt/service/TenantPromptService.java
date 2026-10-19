package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.TenantConfig;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;



@Service
public class TenantPromptService {

    private static final String META_SYSTEM = """
            你是 Prompt 生成引擎，根据客户配置生成定制化的 AI 助手 System Prompt。
            生成的 Prompt 必须：
            1. 准确反映客户的业务场景
            2. 包含必要的约束和边界
            3. 风格与客户的品牌调性一致
            直接输出 System Prompt，不要其他内容。
            """;

    private final DashScopeChatModel chatModel;

    public TenantPromptService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String generateTenantPrompt(TenantConfig config) {
        String request = String.format("""
                生成一个 AI 助手的 System Prompt，配置如下：
                
                公司名称：%s
                业务类型：%s
                助手功能：%s
                不能处理的问题：%s
                语言风格：%s
                特殊要求：%s
                """,
                config.companyName(),
                config.businessType(),
                String.join("、", config.capabilities()),
                String.join("、", config.restrictions()),
                config.tone(),
                config.specialRequirements());

        // 实际项目里应该缓存到数据库，不用每次重新生成
        return chatModel.call(new Prompt(
                List.of(new SystemMessage(META_SYSTEM), new UserMessage(request))
        )).getResult().getOutput().getText();
    }
}