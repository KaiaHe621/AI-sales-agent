package com.jichi.springaialibaba.controller.know;

import com.jichi.springaialibaba.service.KnowledgeBaseService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base")
public class KnowBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping("/upload")
    public KnowledgeBaseService.IngestResult upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "通用") String category) throws Exception {

        Path tempFile = Files.createTempFile("upload-", ".pdf");
        file.transferTo(tempFile);
        try {
            return knowledgeBaseService.ingestDocument(
                    file.getOriginalFilename(), category,
                    new FileSystemResource(tempFile));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return knowledgeBaseService.ask(request.question(), request.conversationId());
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return knowledgeBaseService.askOnce(question);
    }

    @GetMapping("/search")
    public List<KnowledgeBaseService.SearchResult> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        return knowledgeBaseService.search(query, topK);
    }

    record ChatRequest(String conversationId, String question) {
    }
}