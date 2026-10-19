package com.jichi.langchain4j.service;

import com.jichi.langchain4j.model.SentimentResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface SentimentAnalyzer {

    @SystemMessage("""
            你是情感分析专家。
            分析用户评论的情感，给出情感类别、判断依据和评分。
            """)
    SentimentResult analyze(String review);
}