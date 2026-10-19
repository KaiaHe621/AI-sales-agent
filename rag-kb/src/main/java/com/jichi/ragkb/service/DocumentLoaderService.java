package com.jichi.ragkb.service;

import com.jichi.ragkb.service.loader.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentLoaderService {

    private final Map<String, DocumentParser> parsers;

    public DocumentLoaderService(List<DocumentParser> parserList) {
        // 注入所有解析器实现，按 supportedType 建立索引
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(
                        p -> p.supportedType().toUpperCase(),
                        Function.identity()
                ));
        log.info("已加载文档解析器：{}", parsers.keySet());
    }

    /**
     * 解析文档，根据文件类型自动选择解析器。
     *
     * @param inputStream 文件输入流
     * @param fileName    原始文件名（用于判断类型和日志）
     * @return ParseResult
     */
    public ParseResult load(InputStream inputStream, String fileName) {
        String fileType = detectFileType(fileName).toUpperCase();
        DocumentParser parser = parsers.get(fileType);

        if (parser == null) {
            log.warn("[文档加载] 不支持的文件类型：{}，文件：{}", fileType, fileName);
            return ParseResult.failure("不支持的文件类型：" + fileType +
                    "，目前支持：PDF / DOCX / MD / TXT");
        }

        log.info("[文档加载] 开始解析：fileName={}，type={}", fileName, fileType);
        long start = System.currentTimeMillis();

        ParseResult result = parser.parse(inputStream, fileName);

        long elapsed = System.currentTimeMillis() - start;
        if (result.isSuccess()) {
            log.info("[文档加载] 解析完成：fileName={}，页数={}，耗时={}ms",
                    fileName, result.getTotalPages(), elapsed);
        } else {
            log.warn("[文档加载] 解析失败：fileName={}，原因={}",
                    fileName, result.getErrorMsg());
        }

        return result;
    }

    private String detectFileType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) return "UNKNOWN";
        String ext = fileName.substring(dotIndex + 1).toLowerCase();
        return switch (ext) {
            case "pdf" -> "PDF";
            case "docx" -> "DOCX";
            case "md", "markdown" -> "MD";
            case "txt" -> "TXT";
            default -> ext.toUpperCase();
        };
    }
}