package com.jichi.ragkb.service.splitter;

import com.jichi.ragkb.service.loader.ParseResult;

import java.util.List;

public interface ChunkSplitter {

    /**
     * 将解析结果拆分为若干块。
     *
     * @param parseResult 文档解析结果（含多页内容）
     * @param config      分块参数
     * @return 分块列表
     */
    List<ChunkResult> split(ParseResult parseResult, ChunkConfig config);
}