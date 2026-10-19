package com.jichi.prompt.controller;

import com.jichi.prompt.service.CustomerServiceChatService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer-service")
public class CustomerServiceChatController {

    private final CustomerServiceChatService chatService;

    public CustomerServiceChatController(CustomerServiceChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatApiResponse chat(@RequestBody ChatApiRequest request,
                                HttpSession session) {
        // 用 HTTP Session ID 作为对话会话 ID
        String sessionId = session.getId();

        CustomerServiceChatService.ChatResponse response =
                chatService.chat(sessionId, request.message());

        return new ChatApiResponse(
                response.reply(),
                response.needsHumanTransfer(),
                sessionId
        );
    }

    // 用户主动结束对话（清理会话历史）
    @PostMapping("/end-session")
    public void endSession(HttpSession session) {
        // 清理 ChatMemory（如有需要）
        session.invalidate();
    }

    record ChatApiRequest(String message) {}
    record ChatApiResponse(String reply, boolean transferToHuman, String sessionId) {}
}