package com.jichi.ragkb.service.splitter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkResult {

    /** 在整个文档中的顺序索引（0-based） */
    private int chunkIndex;

    /** 分块内容 */
    private String content;

    /** 来自文档第几页（PDF 专用，其他格式为 1） */
    private Integer pageNum;

    /** 所在章节标题 */
    private String sectionTitle;

    /** 估算的 Token 数 */
    private int estimatedTokens;
}