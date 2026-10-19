package com.jichi.springaialibaba.controller.vector;

import com.jichi.springaialibaba.service.DocumentIngestionService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    private final DocumentIngestionService ingestionService;
    private final VectorStore vectorStore;

    public KnowledgeBaseController(DocumentIngestionService ingestionService,
                                   VectorStore vectorStore) {
        this.ingestionService = ingestionService;
        this.vectorStore = vectorStore;
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "通用") String category) throws Exception {

        String filename = file.getOriginalFilename();
        // MultipartFile 不支持随机访问，PDF 解析器需要可寻址的文件，先存临时文件
        Path tempFile = Files.createTempFile("upload-", ".pdf");
        file.transferTo(tempFile);

        int chunks = ingestionService.ingestPdf(filename, category, new FileSystemResource(tempFile));
        Files.deleteIfExists(tempFile);

        return Map.of(
                "filename", filename,
                "category", category,
                "chunks", chunks,
                "status", "入库成功"
        );
    }

    @PostMapping("/text")
    public Map<String, Object> uploadText(@RequestBody TextDocRequest request) {
        ingestionService.ingestText(request.content(), Map.of(
                "source", request.title(),
                "category", request.category()
        ));
        return Map.of("status", "入库成功");
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(0.6)
                        .build()
        );

        return results.stream()
                .map(doc -> Map.of(
                        "content", doc.getText(),
                        "source", doc.getMetadata().getOrDefault("source", "unknown"),
                        "category", doc.getMetadata().getOrDefault("category", "unknown")
                ))
                .toList();
    }

    record TextDocRequest(String title, String category, String content) {
    }
}