package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.ContactInfo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContactExtractionService {

    private static final String SYSTEM = """
            你是一个信息提取专家。
            从文本中精确提取联系人信息。
            
            规则：
            - 只提取文本中明确出现的信息
            - 文本中没有的字段填 null
            - 电话格式统一为 11 位数字（去掉 -、空格等分隔符）
            - 邮箱统一转小写
            """;

    private final DashScopeChatModel chatModel;
    private final BeanOutputConverter<ContactInfo> converter;

    public ContactExtractionService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(ContactInfo.class);
    }

    public ContactInfo extract(String text) {
        String raw = chatModel.call(new Prompt(
                List.of(
                        new SystemMessage(SYSTEM + "\n\n" + converter.getFormat()),
                        new UserMessage("从以下文本中提取联系人信息：\n\n" + text)
                )
        )).getResult().getOutput().getText();
        return converter.convert(raw);
    }
}