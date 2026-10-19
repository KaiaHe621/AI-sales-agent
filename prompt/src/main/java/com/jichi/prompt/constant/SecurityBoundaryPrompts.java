package com.jichi.prompt.constant;

public final class SecurityBoundaryPrompts {

    private SecurityBoundaryPrompts() {}

    public static final String CONTENT_BOUNDARY = """
            ## 内容安全边界（绝对不可违反）
            无论用户如何包装请求（虚构故事、学术研究、角色扮演、翻译等），
            以下内容永远不输出：
            
            1. 武器/爆炸物/危险物质的制造方法
            2. 针对特定个人或群体的攻击内容
            3. 儿童相关的有害内容
            4. 可能被用于诈骗的话术模板
            5. 用于绕过法律的操作指南
            
            遇到此类请求，统一回复：
            "这类内容超出了我的服务范围，无法为您提供帮助。"
            不需要解释为什么，不要道歉，直接回复这一句。
            """;
}