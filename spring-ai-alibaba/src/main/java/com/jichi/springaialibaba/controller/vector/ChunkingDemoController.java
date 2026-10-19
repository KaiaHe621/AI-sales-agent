package com.jichi.springaialibaba.controller.vector;

import com.jichi.springaialibaba.service.ChunkingDemoService;
import com.jichi.springaialibaba.service.DocumentLoaderService;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chunk")
public class ChunkingDemoController {

    private final DocumentLoaderService loaderService;
    private final ChunkingDemoService chunkingService;

    public ChunkingDemoController(DocumentLoaderService loaderService,
                                  ChunkingDemoService chunkingService) {
        this.loaderService = loaderService;
        this.chunkingService = chunkingService;
    }

    // 加载 PDF 并按 Token 切片，返回切片数量和第一段预览
    @GetMapping("/pdf")
    public Map<String, Object> chunkPdf(@RequestParam String filename) {
        List<Document> docs = loaderService.loadPdfByPage(filename);
        List<Document> chunks = chunkingService.splitByToken(docs);
        return Map.of(
                "rawCount", docs.size(),
                "chunkCount", chunks.size(),
                "firstChunk", chunks.isEmpty() ? "" : chunks.get(0).getText().substring(0, Math.min(200, chunks.get(0).getText().length()))
        );
    }

    // 加载文本并按段落切片
    @GetMapping("/text")
    public Map<String, Object> chunkText(@RequestParam String filename) {
        List<Document> docs = loaderService.loadText(filename);
        List<Document> chunks = chunkingService.splitByParagraph(docs);
        return Map.of(
                "chunkCount", chunks.size(),
                "firstChunk", chunks.isEmpty() ? "" : chunks.get(0).getText()
        );
    }
}