package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface CodeReviewer {

    @SystemMessage("你是代码审查专家")
    @UserMessage("""
            请 review 以下 {{language}} 代码：
            
            ```{{language}}
            {{code}}
            ```
            
            重点关注：{{focusAreas}}
            """)
    String review(
            @V("language") String language,
            @V("code") String code,
            @V("focusAreas") String focusAreas
    );
}