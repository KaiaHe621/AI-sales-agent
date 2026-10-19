package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.entity.AbAssignment;
import com.jichi.prompt.service.PromptAbTestService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class AbTestChatController {

    private final DashScopeChatModel chatModel;
    private final PromptAbTestService abTestService;

    public AbTestChatController(DashScopeChatModel chatModel,
                                PromptAbTestService abTestService) {
        this.chatModel = chatModel;
        this.abTestService = abTestService;
    }

    @PostMapping("/ask")
    public ChatResponse ask(@RequestBody ChatRequest request) {
        // 1. 分配 Prompt 版本
        AbAssignment assignment = abTestService.assignPrompt(
                "exp_customer_service_v2", request.userId());

        // 2. 用分配到的 Prompt 版本创建 ChatClient（用 chatModel 而不是自动注入的 Builder）
        ChatClient client = ChatClient.builder(chatModel)
                .defaultSystem(assignment.promptContent())
                .build();

        // 3. 发起请求
        String response = client.prompt()
                .user(request.message())
                .call()
                .content();

        // 4. 记录这次请求属于哪个实验版本（用于后续分析）
        return new ChatResponse(
                response,
                assignment.variant(),    // 返回给前端，前端收集满意度时带上
                assignment.experimentId()
        );
    }

    // 前端收集到用户评分后回调
    @PostMapping("/feedback")
    public void recordFeedback(@RequestBody FeedbackRequest req) {
        abTestService.recordResult(
                req.experimentId(), req.userId(), req.variant(),
                true, req.rating());
    }

    record ChatRequest(String userId, String message) {
    }

    record ChatResponse(String reply, String variant, String experimentId) {
    }

    record FeedbackRequest(String experimentId, String userId, String variant, int rating) {
    }
}