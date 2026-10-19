package com.jichi.ragkb.service.splitter;

import com.jichi.ragkb.service.loader.ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 结构感知分块：优先在标题/段落边界处断开。
 *
 * 适用场景：结构清晰的文档（技术规范、手册等）。
 * 对于流水文字（新闻、小说）效果不如固定窗口。
 *
 * 核心思路：
 * 1. 按标题行切分为若干"节"
 * 2. 节太大则再用固定窗口切分
 * 3. 节太小则与下一节合并（避免碎片化）
 */
@Component("structureAwareSplitter")
@Slf4j
public class StructureAwareChunkSplitter implements ChunkSplitter {

    // 识别中英文标题行
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^(#{1,3}\\s+|第[一二三四五六七八九十百\\d]+[章节]|[一二三四五六七八九十]+、|\\d+\\.\\d?\\s+)(.{2,60})$"
    );

    private final SlidingWindowChunkSplitter slidingSplitter = new SlidingWindowChunkSplitter();

    @Override
    public List<ChunkResult> split(ParseResult parseResult, ChunkConfig config) {
        // 先用固定窗口分块，然后按标题边界合并或进一步切分
        List<TextSection> sections = extractSections(parseResult);
        List<ChunkResult> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (TextSection section : sections) {
            // 节太小（少于50字符）且下一节不是标题开头，合并（在 extractSections 时处理）
            if (section.text().length() <= config.getChunkSize()) {
                // 节大小合适，直接作为一块
                chunks.add(ChunkResult.builder()
                        .chunkIndex(chunkIndex++)
                        .content(section.text())
                        .pageNum(section.pageNum())
                        .sectionTitle(section.title())
                        .estimatedTokens(estimateTokens(section.text()))
                        .build());
            } else {
                // 节太大，降级到固定窗口切分
                ParseResult sectionResult = ParseResult.builder()
                        .success(true)
                        .pages(List.of(ParseResult.PageContent.builder()
                                .pageNum(section.pageNum())
                                .text(section.text())
                                .sectionTitle(section.title())
                                .build()))
                        .totalPages(1)
                        .build();

                List<ChunkResult> subChunks = slidingSplitter.split(sectionResult, config);
                for (ChunkResult sub : subChunks) {
                    sub.setChunkIndex(chunkIndex++);
                    if (sub.getSectionTitle() == null) sub.setSectionTitle(section.title());
                    chunks.add(sub);
                }
            }
        }

        return chunks;
    }

    private List<TextSection> extractSections(ParseResult parseResult) {
        List<TextSection> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String currentTitle = null;
        int currentPage = 1;

        for (ParseResult.PageContent page : parseResult.getPages()) {
            String[] lines = page.getText().split("\n");
            for (String line : lines) {
                var matcher = HEADING_PATTERN.matcher(line.strip());
                if (matcher.matches() && current.length() > 50) {
                    // 遇到标题且当前节有内容，保存
                    sections.add(new TextSection(currentTitle, current.toString().strip(), currentPage));
                    current = new StringBuilder();
                    currentTitle = line.strip();
                    currentPage = page.getPageNum();
                }
                current.append(line).append("\n");
            }
            currentPage = page.getPageNum();
        }

        if (!current.isEmpty()) {
            sections.add(new TextSection(currentTitle, current.toString().strip(), currentPage));
        }

        return sections;
    }

    private int estimateTokens(String text) {
        if (text == null) return 0;
        int chinese = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') chinese++;
            else if (!Character.isWhitespace(c)) other++;
        }
        return (int) (chinese * 1.5 + other * 0.3);
    }

    record TextSection(String title, String text, int pageNum) {}
}