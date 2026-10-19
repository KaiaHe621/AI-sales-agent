package com.jichi.ragkb.service.splitter;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkConfig {

    /** 每块最大字符数 */
    @Builder.Default
    private int chunkSize = 512;

    /** 相邻块的重叠字符数，避免信息在块边界被截断 */
    @Builder.Default
    private int chunkOverlap = 64;

    /** 是否启用结构感知分块（按段落/标题断点） */
    @Builder.Default
    private boolean structureAware = false;

    public static ChunkConfig defaultConfig() {
        return ChunkConfig.builder().build();
    }
}