package com.jichi.prompt;

import com.jichi.prompt.service.TicketClassificationService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.util.AssertionErrors.assertEquals;

@SpringBootTest
class ClassificationPromptTest {

    @Autowired
    private TicketClassificationService classificationService;

    @ParameterizedTest
    @CsvSource({
        "'我的信用卡被扣了两次', BILLING",
        "'登录页面报错500', TECH_SUPPORT",
        "'希望增加批量导出功能', FEATURE_REQUEST",
        "'账号被锁了忘记密码', ACCOUNT",
        "'表扬一下客服态度好', OTHER"
    })
    void testClassification(String ticket, String expected) {
        String result = classificationService.classify(ticket);
        assertEquals("工单「" + ticket + "」应分类为 " + expected + "，实际：" + result,expected, result.trim());
    }
}