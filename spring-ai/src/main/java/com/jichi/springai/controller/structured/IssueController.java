package com.jichi.springai.controller.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/issue")
public class IssueController {

    enum Priority {LOW, MEDIUM, HIGH, CRITICAL}

    enum Category {BUG, FEATURE, IMPROVEMENT, DOCUMENTATION}

    record IssueClassification(
            String title,
            Category category,
            Priority priority,
            String assignTo,
            String reason
    ) {
    }

    record IssueRequest(String description) {
    }

    private final ChatClient chatClient;

    public IssueController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/classify")
    public IssueClassification classify(@RequestBody IssueRequest request) {
        return chatClient.prompt()
                .system("你是项目经理，负责对 Issue 进行分类和优先级评估。")
                .user("请对这个 Issue 进行分类：" + request.description())
                .call()
                .entity(IssueClassification.class);
    }
}