package com.jichi.prompt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.prompt.service.SentimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
class SentimentPromptTestFromFile {

    @Autowired
    private SentimentService sentimentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void runAllTestCases() throws Exception {
        Resource resource = new ClassPathResource("test-cases/sentiment-test-cases.json");
        List<TestCase> cases = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<List<TestCase>>() {});

        int passed = 0, failed = 0;
        List<String> failures = new ArrayList<>();

        for (TestCase tc : cases) {
            String actual = sentimentService.analyze(tc.input()).trim();
            if (actual.equals(tc.expectedLabel())) {
                passed++;
            } else {
                failed++;
                failures.add(String.format("[%s] 期望：%s，实际：%s，输入：%s",
                        tc.id(), tc.expectedLabel(), actual, tc.input()));
            }
        }

        System.out.printf("测试结果：通过 %d，失败 %d，通过率 %.1f%%%n",
                passed, failed, (double) passed / cases.size() * 100);

        if (!failures.isEmpty()) {
            fail("以下用例失败：\n" + String.join("\n", failures));
        }
    }

    record TestCase(String id, String description, String input,
                    String expectedLabel, List<String> tags) {}
}