package com.jichi.ragkb.service.splitter;

import com.jichi.ragkb.service.loader.ParseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定窗口滑动分块。
 *
 * 核心逻辑：
 * 1. 将文档所有页的文本合并成一个大字符串
 * 2. 按 chunkSize 滑动，步长 = chunkSize - chunkOverlap
 * 3. 尽量在句子/段落边界处断开，避免在句子中间截断
 */
@Component
@Slf4j
public class SlidingWindowChunkSplitter implements ChunkSplitter {

    @Override
    public List<ChunkResult> split(ParseResult parseResult, ChunkConfig config) {
        List<ChunkResult> chunks = new ArrayList<>();
        int chunkIndex = 0;

        for (ParseResult.PageContent page : parseResult.getPages()) {
            String text = page.getText();
            if (text == null || text.isBlank()) continue;

            List<String> pageChunks = splitText(text, config.getChunkSize(), config.getChunkOverlap());

            for (String chunkText : pageChunks) {
                if (chunkText.isBlank()) continue;

                chunks.add(ChunkResult.builder()
                        .chunkIndex(chunkIndex++)
                        .content(chunkText)
                        .pageNum(page.getPageNum())
                        .sectionTitle(page.getSectionTitle())
                        .estimatedTokens(estimateTokens(chunkText))
                        .build());
            }
        }

        log.debug("[分块] 文档分块完成，共{}块，avgSize={}字符",
                chunks.size(),
                chunks.isEmpty() ? 0 : chunks.stream()
                        .mapToInt(c -> c.getContent().length()).average().orElse(0));

        return chunks;
    }

    /**
     * 核心分块逻辑，在句子边界处断开。
     */
    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // 如果没有到文本末尾，尝试在句子/段落边界处断开
            if (end < text.length()) {
                end = findGoodBreakPoint(text, end, overlap);
            }

            String chunk = text.substring(start, end).strip();
            if (!chunk.isBlank()) {
                result.add(chunk);
            }

            int nextStart = end - overlap;
            if (nextStart <= start) {
                nextStart = end;
            }
            start = nextStart;
        }

        return result;
    }

    /**
     * 从 position 向前找到一个好的断点（段落 > 句号 > 逗号 > 空格）。
     * 最多回退 100 字符，找不到就直接断。
     */
    private int findGoodBreakPoint(String text, int position, int overlap) {
        int searchRange = Math.min(100, position - overlap);  // 不能回退太多

        // 优先级：段落换行 > 句号/问号/感叹号 > 分号/逗号 > 空格
        String[] breakChars = {"\n\n", "\n", "。", "！", "？", "；", "，", " "};

        for (String breakChar : breakChars) {
            int idx = text.lastIndexOf(breakChar, position);
            if (idx > position - searchRange && idx > 0) {
                return idx + breakChar.length();
            }
        }

        return position;  // 找不到好的断点，直接截断
    }

    /**
     * 简单的 Token 估算：中文每字约 1.5 Token，英文每字符约 0.3 Token。
     * 不依赖外部 Tokenizer，近似计算。
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                chineseChars++;
            } else if (!Character.isWhitespace(c)) {
                otherChars++;
            }
        }
        return (int) (chineseChars * 1.5 + otherChars * 0.3);
    }
}