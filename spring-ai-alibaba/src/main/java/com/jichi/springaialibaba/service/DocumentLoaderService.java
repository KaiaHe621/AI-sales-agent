package com.jichi.springaialibaba.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentLoaderService {

    // PDF 按页读（每页一个 Document，适合内容按页划分的文档）
    public List<Document> loadPdfByPage(String classpathFile) {
        return new PagePdfDocumentReader(new ClassPathResource(classpathFile)).get();
    }

    // PDF 连续读（不按页，适合跨页的长段落）
    public List<Document> loadPdfByParagraph(String classpathFile) {
        return new ParagraphPdfDocumentReader(new ClassPathResource(classpathFile)).get();
    }

    // Word/Excel/HTML/TXT 等——用 Apache Tika，基本什么格式都能解
    public List<Document> loadWithTika(String absolutePath) {
        return new TikaDocumentReader(new FileSystemResource(absolutePath)).get();
    }

    // 纯文本
    public List<Document> loadText(String classpathFile) {
        return new TextReader(new ClassPathResource(classpathFile)).get();
    }
}