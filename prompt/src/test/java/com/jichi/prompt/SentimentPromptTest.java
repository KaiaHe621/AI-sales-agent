package com.jichi.prompt;

import com.jichi.prompt.service.SentimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SentimentPromptTest {

    @Autowired
    private SentimentService sentimentService;

    // 正向测试：典型正面评论
    @Test
    void testPositiveSentiment() {
        String result = sentimentService.analyze("东西很好，下次还会购买");
        System.out.println(result);
        assertEquals("POSITIVE", result.trim());
    }

    // 负向测试：典型负面评论
    @Test
    void testNegativeSentiment() {
        String result = sentimentService.analyze("质量差，完全不值这个价");
        assertEquals("NEGATIVE", result.trim());
    }

    // 边界测试：混合情感
    @Test
    void testMixedSentiment() {
        String result = sentimentService.analyze("东西不错但价格贵");
        assertTrue(result.trim().equals("MIXED") || result.trim().equals("POSITIVE"),
                "混合情感评论应为 MIXED 或 POSITIVE，实际：" + result);
    }

    // 边界测试：空输入
    @Test
    void testEmptyInput() {
        String result = sentimentService.analyze("");
        assertNotNull(result);
        assertFalse(result.isBlank(), "空输入不应返回空白结果");
    }

    // 边界测试：无关内容（越界请求）
    @Test
    void testOffTopicInput() {
        String result = sentimentService.analyze("帮我写一段 Python 代码");
        // 情感分类助手应该拒绝或返回 NEUTRAL/UNKNOWN，不应该去写代码
        assertFalse(result.contains("def ") || result.contains("print("),
                "情感分析助手不应该响应代码生成请求");
    }
}