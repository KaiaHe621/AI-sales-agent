package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CodeReviewService {

    private final DashScopeChatModel chatModel;

    @Value("classpath:prompts/code-review.st")
    private Resource codeReviewPromptResource;

    public CodeReviewService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String review(String code, String language) {
        PromptTemplate pt = new PromptTemplate(codeReviewPromptResource);
        String userPrompt = pt.render(Map.of(
                "language", language,
                "code", code
        ));

        return chatModel.call(new Prompt(
                List.of(
                        new SystemMessage("""
                                你是一个资深工程师，专注代码质量。
                                找出 Bug、性能问题和最佳实践违反，每个问题标注严重程度。
                                """),
                        new UserMessage(userPrompt)
                )
        )).getResult().getOutput().getText();
    }
}