package com.jichi.springaialibaba.controller.parallel;

import com.jichi.springaialibaba.service.VirtualThreadChatService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/virtual")
public class VirtualThreadChatController {

    private final VirtualThreadChatService virtualThreadChatService;

    public VirtualThreadChatController(VirtualThreadChatService virtualThreadChatService) {
        this.virtualThreadChatService = virtualThreadChatService;
    }

    // 赛马模式：谁先回就用谁
    @GetMapping("/fastest")
    public String fastest(@RequestParam String question) throws Exception {
        return virtualThreadChatService.fastestResponse(question);
    }

    // 全量模式：所有模型都回，一起返回
    @GetMapping("/all")
    public Map<String, String> all(@RequestParam String question) throws Exception {
        return virtualThreadChatService.allResponses(question);
    }
}