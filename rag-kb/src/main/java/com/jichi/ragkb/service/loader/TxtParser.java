package com.jichi.ragkb.service.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class TxtParser implements DocumentParser {

    /**
     * UTF-8 解码后若替换字符（U+FFFD）占比超过阈值，则判定为非 UTF-8，降级到 GBK
     */
    private static final double UTF8_DECODE_FAIL_THRESHOLD = 0.01;

    /**
     * 非可打印控制字符占比阈值——超过即判定为二进制文件
     */
    private static final double BINARY_THRESHOLD = 0.05;

    @Override
    public String supportedType() {
        return "TXT";
    }

    @Override
    public ParseResult parse(InputStream inputStream, String fileName) {
        try {
            byte[] bytes = inputStream.readAllBytes();

            // 1. 先尝试 UTF-8，乱码率高则降级到 GBK（覆盖国内常见的 ANSI/GBK 文件）
            Charset charset = StandardCharsets.UTF_8;
            String text = new String(bytes, charset);
            long replacementCount = text.chars().filter(c -> c == 0xFFFD).count();
            if (replacementCount > text.length() * UTF8_DECODE_FAIL_THRESHOLD) {
                charset = Charset.forName("GBK");
                text = new String(bytes, charset);
                log.info("[TXT解析] 文件={} 非 UTF-8，降级到 GBK 解码", fileName);
            }

            // 2. 去除 UTF-8 BOM（U+FEFF），避免污染正文
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }

            // 3. 统一换行符
            text = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

            if (text.isBlank()) {
                return ParseResult.failure("文本文件内容为空");
            }

            // 4. 二进制文件保护：非可打印控制字符占比过高，判定为非文本
            if (isLikelyBinary(text)) {
                return ParseResult.failure("文件包含大量非文本字符，疑似二进制文件");
            }

            log.info("[TXT解析] 文件={}，编码={}，字符数={}", fileName, charset.name(), text.length());

            return ParseResult.builder()
                    .success(true)
                    .pages(List.of(ParseResult.PageContent.builder()
                            .pageNum(1)
                            .text(text.strip())
                            .build()))
                    .totalPages(1)
                    .build();

        } catch (Exception e) {
            log.error("[TXT解析] 文件={}，解析失败：{}", fileName, e.getMessage(), e);
            return ParseResult.failure("TXT 解析失败：" + e.getMessage());
        }
    }

    /**
     * 简单启发式：非换行/制表符的控制字符占比超过阈值，视为二进制文件
     */
    private boolean isLikelyBinary(String text) {
        long nonPrintable = text.chars()
                .filter(c -> Character.isISOControl(c) && c != '\n' && c != '\t' && c != '\r')
                .count();
        return nonPrintable > text.length() * BINARY_THRESHOLD;
    }
}