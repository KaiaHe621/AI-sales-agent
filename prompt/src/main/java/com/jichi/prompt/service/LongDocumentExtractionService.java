package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.ContractInfo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LongDocumentExtractionService {

    private final DashScopeChatModel chatModel;
    private final BeanOutputConverter<ContractInfo> contractConverter;

    private static final int MAX_CHUNK_SIZE = 3000;

    public LongDocumentExtractionService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
        this.contractConverter = new BeanOutputConverter<>(ContractInfo.class);
    }

    /**
     * 分段提取关键信息，最后合并
     */
    public ContractInfo extractLongDocument(String fullText) {
        List<String> chunks = splitIntoChunks(fullText, MAX_CHUNK_SIZE);

        List<String> chunkResults = chunks.stream()
                .map(this::extractChunk)
                .toList();

        return mergeResults(chunkResults);
    }

    private String extractChunk(String chunk) {
        return chatModel.call(new Prompt(
                List.of(
                        new SystemMessage("""
                                你正在处理一份长文档的片段。
                                从这个片段中提取能找到的关键信息。
                                如果某个字段在这个片段里没有，填 null。
                                只提取，不要猜测。
                                """),
                        new UserMessage("从以下文档片段提取合同关键信息：\n\n" + chunk)
                )
        )).getResult().getOutput().getText();
    }

    /**
     * 把多个片段的提取结果合并，用 AI 做最终整合
     */
    private ContractInfo mergeResults(List<String> partialResults) {
        String mergePrompt = String.format("""
                以下是从一份合同不同部分提取的信息片段，请合并成完整的合同信息。
                合并规则：
                - 如果多个片段对同一字段有不同值，选择更完整/更具体的那个
                - 列表类字段（如 keyObligations）合并所有片段的内容并去重
                - 仍然缺少的字段填 null
                
                各片段提取结果：
                %s
                
                %s
                """, partialResults.toString(), contractConverter.getFormat());

        String raw = chatModel.call(new Prompt(
                new UserMessage(mergePrompt)
        )).getResult().getOutput().getText();

        return contractConverter.convert(raw);
    }

    private List<String> splitIntoChunks(String text, int maxSize) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");
        StringBuilder current = new StringBuilder();

        for (String para : paragraphs) {
            if (current.length() + para.length() > maxSize && current.length() > 0) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            current.append(para).append("\n\n");
        }

        if (!current.isEmpty()) chunks.add(current.toString());
        return chunks;
    }
}