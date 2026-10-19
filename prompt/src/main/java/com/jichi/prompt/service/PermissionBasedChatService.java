package com.jichi.prompt.service;

import com.jichi.prompt.enums.UserRole;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PermissionBasedChatService {

    private final ChatClient guestClient;
    private final ChatClient memberClient;

    public PermissionBasedChatService(
            @Qualifier("guestClient") ChatClient guestClient,
            @Qualifier("memberClient") ChatClient memberClient) {
        this.guestClient = guestClient;
        this.memberClient = memberClient;
    }

    public ChatClient getClientForUser(UserRole role) {
        return switch (role) {
            case GUEST -> guestClient;
            case MEMBER, PREMIUM, ADMIN -> memberClient;
        };
    }
}