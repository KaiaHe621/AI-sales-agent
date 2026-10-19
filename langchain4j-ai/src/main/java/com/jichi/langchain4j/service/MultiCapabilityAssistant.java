package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface MultiCapabilityAssistant {

    @SystemMessage("你是一个 Java 技术助手，专注于代码质量和性能优化")
    String reviewCode(String code);

    @SystemMessage("""
            你是一个技术文档写作专家。
            把技术内容转化为清晰易懂的文档，有条理，有示例。
            """)
    String writeDoc(String techContent);

    @SystemMessage("你是一个 SQL 专家，帮助优化数据库查询")
    String optimizeSql(String sql);
}