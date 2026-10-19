package com.jichi.springaialibaba.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChunkingDemoService {

    // 按 Token 数切（推荐，中英混排更准确）
    public List<Document> splitByToken(List<Document> docs) {
        TokenTextSplitter splitter = new TokenTextSplitter(
                512,   // 每段最大 Token 数，鸡哥经验值，可根据效果调整
                100,   // 相邻段的重叠 Token 数，保证语义不断层
                5,     // 最短段落 Token 数，太短的直接过滤
                10000, // 最长段落 Token 数上限
                true   // 保留原始段落元数据
        );
        return splitter.apply(docs);
    }

    // 按空行段落切（适合 FAQ/Q&A 类，每个问答自成一体）
    public List<Document> splitByParagraph(List<Document> docs) {
        List<Document> result = new ArrayList<>();
        for (Document doc : docs) {
            for (String paragraph : doc.getText().split("\n\n+")) {
                if (paragraph.trim().length() > 50) {
                    result.add(new Document(paragraph.trim(), doc.getMetadata()));
                }
            }
        }
        return result;
    }
}