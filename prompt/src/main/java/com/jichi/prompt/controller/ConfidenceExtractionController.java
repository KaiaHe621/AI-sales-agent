package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.ContactInfoWithConfidence;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/extract")
public class ConfidenceExtractionController {

    private static final String SYSTEM = """
            从文本中提取联系人信息，同时为每个字段提供置信度（0.0-1.0）：
            - 1.0：文本中明确写明
            - 0.7-0.9：可以高度推断
            - 0.5-0.7：有一定依据但不确定
            - 低于 0.5：不建议使用
            """;

    private final DashScopeChatModel chatModel;
    private final BeanOutputConverter<ContactInfoWithConfidence> converter;

    public ConfidenceExtractionController(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(ContactInfoWithConfidence.class);
    }

    @PostMapping("/contact-with-confidence")
    public ContactInfoWithConfidence extractWithConfidence(@RequestBody String text) {
        String raw = chatModel.call(new Prompt(
                List.of(
                        new SystemMessage(SYSTEM + "\n\n" + converter.getFormat()),
                        new UserMessage("提取联系人信息（含置信度）：\n\n" + text)
                )
        )).getResult().getOutput().getText();
        return converter.convert(raw);
    }
}