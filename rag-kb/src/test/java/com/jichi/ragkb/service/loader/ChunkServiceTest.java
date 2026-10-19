package com.jichi.ragkb.service.loader;

import com.jichi.ragkb.service.ChunkService;
import com.jichi.ragkb.service.loader.ParseResult;
import com.jichi.ragkb.service.splitter.ChunkResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ChunkServiceTest {

    @Autowired
    private ChunkService chunkService;

    @Test
    void chunksNotTooLargeOrTooSmall() {
        String longText = "这是一段测试文本。".repeat(200); // 约 1800 字符
        ParseResult result = ParseResult.builder()
                .success(true)
                .pages(List.of(ParseResult.PageContent.builder()
                        .pageNum(1)
                        .text(longText)
                        .build()))
                .totalPages(1)
                .build();

        List<ChunkResult> chunks = chunkService.chunk(result);

        assertThat(chunks).isNotEmpty();
        for (ChunkResult chunk : chunks) {
            // 每块不应超过 chunkSize 的 1.2 倍（允许少量超出用于句子边界对齐）
            assertThat(chunk.getContent().length()).isLessThanOrEqualTo(620);
            // 每块至少 20 字符
            assertThat(chunk.getContent().length()).isGreaterThanOrEqualTo(20);
        }

        // 验证相邻块有重叠
        if (chunks.size() >= 2) {
            String end0 = chunks.get(0).getContent();
            String start1 = chunks.get(1).getContent();
            // 第一块末尾的内容应该出现在第二块开头附近（重叠）
            String overlapPart = end0.substring(Math.max(0, end0.length() - 64));
            assertThat(start1).contains(overlapPart.substring(0, Math.min(30, overlapPart.length())));
        }
    }
}