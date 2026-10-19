package com.jichi.voiceshopping.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug/sa")
public class SaDebugController {

    /** 按指定 token 反查登录态，不依赖当前请求 header */
    @GetMapping("/who")
    public Map<String, Object> who(@RequestParam String token) {
        Object loginId = StpUtil.getLoginIdByToken(token);
        Map<String, Object> ret = new HashMap<>();
        ret.put("valid", loginId != null);
        ret.put("loginId", loginId);
        ret.put("tokenTimeout", StpUtil.getTokenTimeout(token));
        return ret;
    }

    /** 从当前请求 header/cookie 读 token，验证 Sa-Token 读取链路 */
    @GetMapping("/me")
    public Map<String, Object> me() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("isLogin", StpUtil.isLogin());
        ret.put("tokenValue", StpUtil.getTokenValue());
        ret.put("loginId", StpUtil.getLoginIdDefaultNull());
        return ret;
    }
}