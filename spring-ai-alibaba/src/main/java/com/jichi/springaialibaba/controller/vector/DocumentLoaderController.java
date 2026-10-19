package com.jichi.springaialibaba.controller.vector;

import com.jichi.springaialibaba.service.DocumentLoaderService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loader")
public class DocumentLoaderController {

    private final DocumentLoaderService loaderService;

    public DocumentLoaderController(DocumentLoaderService loaderService) {
        this.loaderService = loaderService;
    }

    // 测试 PDF 按页加载，返回段落数量和第一段预览
    @GetMapping("/pdf")
    public Map<String, Object> loadPdf(@RequestParam String filename) {
        List<Document> docs = loaderService.loadPdfByPage(filename);
        return Map.of(
                "count", docs.size(),
                "first", docs.isEmpty() ? "" : docs.get(0).getText().substring(0, Math.min(200, docs.get(0).getText().length()))
        );
    }

    // 测试纯文本加载
    @GetMapping("/text")
    public Map<String, Object> loadText(@RequestParam String filename) {
        List<Document> docs = loaderService.loadText(filename);
        return Map.of(
                "count", docs.size(),
                "content", docs.isEmpty() ? "" : docs.get(0).getText()
        );
    }
}