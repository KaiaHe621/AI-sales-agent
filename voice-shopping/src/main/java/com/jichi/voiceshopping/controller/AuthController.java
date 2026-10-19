package com.jichi.voiceshopping.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.jichi.voiceshopping.entity.AppUserEntity;
import com.jichi.voiceshopping.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository userRepo;

    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        Long userId = validateAndGetUserId(req);
        StpUtil.login(userId);
        return Map.of("token", StpUtil.getTokenValue(), "userId", userId);
    }

    private Long validateAndGetUserId(LoginRequest req) {
        if (req == null || req.username() == null || req.username().isBlank()) {
            throw new ResponseStatusException(UNAUTHORIZED, "username 不能为空");
        }
        AppUserEntity user = userRepo.findByUsernameAndStatus(req.username(), "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "用户不存在或已注销"));
        return user.getId();
    }
}