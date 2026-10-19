package com.jichi.prompt.entity;

import com.jichi.prompt.enums.UserRole;

public record PermissionChatRequest(String message, UserRole role) {}