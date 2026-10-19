package com.jichi.agentscope.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class AsyncSearchTool {

    @Tool(name = "search_web", description = "搜索互联网上的最新信息")
    public Mono<String> searchWeb(
            @ToolParam(name = "query", description = "搜索关键词") String query
    ) {
        // 模拟异步查询，实际项目换成 WebClient 调用外部搜索 API
        return Mono.fromCallable(() ->
                String.format("关于%s的搜索结果：找到 3 篇相关文章，最新发布于 2025-06-01。", query)
        );
    }
}