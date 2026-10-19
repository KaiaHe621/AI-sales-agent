package com.jichi.salesAgent;

import com.jichi.salesAgent.agent.SalesAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

@SpringBootTest
class SalesAgentSmokeTest {

    @Autowired
    private SalesAgent salesAgent;

    // chat() 的第三个参数 today 对应 System Prompt 里的 {{today}} 变量
    private final String today = LocalDate.now().toString();

    @Test
    void smokeTest() {
        String response = salesAgent.chat(
                "test-session-001",
                "你好，你能做什么？",
                today);
        System.out.println("Agent 回答：" + response);
    }

    @Test
    void toolCallTest() {
        String response = salesAgent.chat(
                "test-session-002",
                "近6个月的月度销售趋势是什么？",
                today);
        System.out.println("Agent 回答：" + response);
    }
}