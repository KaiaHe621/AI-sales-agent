package com.jichi.ragkb.service;

import com.jichi.ragkb.service.loader.ParseResult;
import com.jichi.ragkb.service.splitter.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkService {

    private final SlidingWindowChunkSplitter slidingWindowSplitter;
    private final StructureAwareChunkSplitter structureAwareSplitter;

    @Value("${rag.chunk.size:512}")
    private int defaultChunkSize;

    @Value("${rag.chunk.overlap:64}")
    private int defaultOverlap;

    /**
     * 对解析结果进行分块。
     * 如果文档有清晰的章节结构，使用结构感知分块；否则使用固定窗口分块。
     */
    public List<ChunkResult> chunk(ParseResult parseResult) {
        ChunkConfig config = ChunkConfig.builder()
                .chunkSize(defaultChunkSize)
                .chunkOverlap(defaultOverlap)
                .build();

        return chunk(parseResult, config);
    }

    public List<ChunkResult> chunk(ParseResult parseResult, ChunkConfig config) {
        if (parseResult == null || !parseResult.isSuccess()) {
            return List.of();
        }

        // 判断是否应该用结构感知分块：文档有明显标题结构
        boolean hasStructure = parseResult.getPages().stream()
                .anyMatch(p -> p.getSectionTitle() != null);

        ChunkSplitter splitter = (hasStructure && config.isStructureAware())
                ? structureAwareSplitter
                : slidingWindowSplitter;

        List<ChunkResult> chunks = splitter.split(parseResult, config);

        // 过滤掉太短的块（少于 20 字符的碎片没有检索价值）
        chunks = chunks.stream()
                .filter(c -> c.getContent().length() >= 20)
                .toList();

        log.info("[分块] 完成分块：策略={}，共{}块，总字符={}",
                splitter.getClass().getSimpleName(),
                chunks.size(),
                chunks.stream().mapToInt(c -> c.getContent().length()).sum());

        return chunks;
    }
}