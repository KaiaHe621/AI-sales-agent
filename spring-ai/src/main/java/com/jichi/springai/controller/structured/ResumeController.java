package com.jichi.springai.controller.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/resume")
public class ResumeController {

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

    public ResumeController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/analyze")
    public ResumeAnalysis analyze(@RequestBody ResumeRequest request) {
        return chatClient.prompt()
                .system("你是一个专业的HR，帮助分析候选人简历。字段为空时填null，技能等级只能是：入门/熟练/精通。")
                .user("分析这份简历：\n" + request.content())
                .call()
                .entity(ResumeAnalysis.class);
    }
}