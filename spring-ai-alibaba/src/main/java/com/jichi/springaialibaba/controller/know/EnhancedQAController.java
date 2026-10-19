package com.jichi.springaialibaba.controller.know;

import com.jichi.springaialibaba.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge-base")
public class EnhancedQAController {

    private final KnowledgeBaseService knowledgeBaseService;

    public EnhancedQAController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @GetMapping("/ask-enhanced")
    public String askEnhanced(@RequestParam String question) {
        // 先用模型改写成更规范的检索语句
        String rewrittenQuestion = knowledgeBaseService.askOnce(
                "将以下问题改写成更规范、适合文档检索的表述，只输出改写后的问题，不要解释：" + question);

        // 用改写后的问题做 RAG 问答
        return knowledgeBaseService.askOnce(rewrittenQuestion);
    }
}