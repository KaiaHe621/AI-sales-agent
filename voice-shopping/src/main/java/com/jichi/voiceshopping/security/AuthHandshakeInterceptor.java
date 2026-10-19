package com.jichi.voiceshopping.security;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class AuthHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler handler,
                                   Map<String, Object> attributes) {
        String protocol = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (protocol == null || !protocol.startsWith("Bearer-")) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        String token = protocol.substring("Bearer-".length());
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put("userId", Long.parseLong(loginId.toString()));
        attributes.put("sessionId", getParam(request, "sessionId"));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest r, ServerHttpResponse rp,
                               WebSocketHandler h, Exception e) {
    }

    private String getParam(ServerHttpRequest r, String name) {
        // ... 解析 query params
        return "";
    }
}