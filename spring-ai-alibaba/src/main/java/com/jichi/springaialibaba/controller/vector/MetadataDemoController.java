package com.jichi.springaialibaba.controller.vector;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metadata")
public class MetadataDemoController {

    private final VectorStore vectorStore;

    public MetadataDemoController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // 手动写入一条带元数据的文档，演示 metadata 的结构
    @PostMapping("/add")
    public Map<String, Object> addWithMetadata() {
        Document doc = new Document(
                "购买后 7 天内可无理由退货，商品需保持原包装。",
                Map.of(
                        "source",     "退货政策.pdf",
                        "page",       "3",
                        "category",   "售后政策",
                        "updated_at", "2024-01-01",
                        "doc_id",     "policy-001"
                )
        );
        vectorStore.add(List.of(doc));
        return Map.of("status", "写入成功", "docId", "policy-001");
    }
}