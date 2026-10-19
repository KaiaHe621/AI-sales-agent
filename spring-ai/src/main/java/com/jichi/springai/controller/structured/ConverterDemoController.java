package com.jichi.springai.controller.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/converter")
public class ConverterDemoController {

    record SkillLevel(String name, String level) {
    }

    record ResumeAnalysis(
            String name,
            String email,
            String summary,
            List<SkillLevel> technicalSkills,
            List<String> workHistory,
            String overallAssessment
    ) {
    }

    record ResumeRequest(String content) {
    }

    private final ChatClient chatClient;

    public ConverterDemoController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/analyze")
    public ResumeAnalysis analyze(@RequestBody ResumeRequest request) {
        BeanOutputConverter<ResumeAnalysis> converter =
                new BeanOutputConverter<>(ResumeAnalysis.class);

        // 查看自动生成的 JSON Schema（开发调试时可以打印出来看）
        // System.out.println(converter.getFormat());

        String prompt = """
                分析这份简历，按照以下 JSON 格式输出：
                %s
                
                简历内容：%s
                """.formatted(converter.getFormat(), request.content());

        String jsonResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return converter.convert(jsonResponse);
    }
}