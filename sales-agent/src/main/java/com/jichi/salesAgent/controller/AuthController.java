package com.jichi.salesAgent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.jichi.salesAgent.entity.SalesRep;
import com.jichi.salesAgent.repository.SalesRepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SalesRepRepository repRepository;

    record LoginRequest(Long repId) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<SalesRep> repOpt = repRepository.findById(request.repId());
        if (repOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("用户不存在");
        }
        SalesRep rep = repOpt.get();

        // Sa-Token 登录，repId 作为登录标识
        StpUtil.login(rep.getId());

        // 把用户信息写入 Sa-Token Session，供后续请求使用
        StpUtil.getSession()
                .set("username", rep.getName())
                .set("role",     rep.getRole())
                .set("regionId", rep.getRegionId())
                .set("repId",    rep.getId());

        return ResponseEntity.ok(Map.of(
                "token",    StpUtil.getTokenValue(),
                "username", rep.getName(),
                "role",     rep.getRole()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        StpUtil.logout();
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }
}
