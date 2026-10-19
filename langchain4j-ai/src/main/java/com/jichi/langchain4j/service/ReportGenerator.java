package com.jichi.langchain4j.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ReportGenerator {

    @SystemMessage("你是一个数据分析专家，擅长生成业务报告")
    @UserMessage("""
            根据以下数据生成一份{{reportType}}报告：
            
            时间范围：{{startDate}} 至 {{endDate}}
            数据：
            {{data}}
            
            报告要求：
            - 字数：{{wordCount}} 字左右
            - 重点分析：{{focus}}
            """)
    String generateReport(
            @V("reportType") String reportType,
            @V("startDate") String startDate,
            @V("endDate") String endDate,
            @V("data") String data,
            @V("wordCount") int wordCount,
            @V("focus") String focus
    );
}