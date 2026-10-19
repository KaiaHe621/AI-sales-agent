package com.jichi.langchain4j.controller.chat;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
public class SimpleChatController {

    private final ChatModel chatModel;

    public SimpleChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return chatModel.chat(message);
    }

}