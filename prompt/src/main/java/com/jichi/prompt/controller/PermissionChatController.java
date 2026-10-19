package com.jichi.prompt.controller;

import com.jichi.prompt.entity.PermissionChatRequest;
import com.jichi.prompt.service.PermissionBasedChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/permission-chat")
public class PermissionChatController {

    private final PermissionBasedChatService permissionService;

    public PermissionChatController(PermissionBasedChatService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping("/ask")
    public String chat(@RequestBody PermissionChatRequest req) {
        return permissionService.getClientForUser(req.role())
                .prompt()
                .user(req.message())
                .call()
                .content();
    }
}