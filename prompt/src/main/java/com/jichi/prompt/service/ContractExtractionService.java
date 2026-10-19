package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.ContractInfo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContractExtractionService {

    private static final String SYSTEM = """
            你是一个合同信息提取专家，有 10 年法律文件分析经验。
            
            提取规则：
            1. 只提取文本中明确表述的信息，不推断不猜测
            2. 日期统一转为 YYYY-MM-DD 格式；"永久"或"无限期"填 null
            3. 金额提取数字部分（去掉"元"、"万元"等单位，但保留单位到 currency 字段）
               例："人民币壹拾万元整" → amount=100000, currency="CNY"
            4. 如果某个字段在文本中没有明确表述，填 null
            5. warnings 字段：记录提取过程中遇到的歧义或注意事项
            """;

    private final DashScopeChatModel chatModel;
    private final BeanOutputConverter<ContractInfo> converter;

    public ContractExtractionService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.converter = new BeanOutputConverter<>(ContractInfo.class);
    }

    public ContractInfo extract(String contractText) {
        String raw = chatModel.call(new Prompt(
                List.of(
                        new SystemMessage(SYSTEM + "\n\n" + converter.getFormat()),
                        new UserMessage("提取以下合同的关键信息：\n\n" + contractText)
                )
        )).getResult().getOutput().getText();
        return converter.convert(raw);
    }
}