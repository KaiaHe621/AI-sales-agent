package com.jichi.voiceshopping.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VoiceHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse resp,
                                   WebSocketHandler handler, Map<String, Object> attr) {
        URI uri = req.getURI();
        Map<String, String> params = parseQuery(uri.getQuery());
        attr.put("userId", Long.parseLong(params.getOrDefault("userId", "0")));
        attr.put("sessionId", params.getOrDefault("sessionId", UUID.randomUUID().toString()));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest req, ServerHttpResponse resp,
                               WebSocketHandler handler, Exception exception) {
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> m = new HashMap<>();
        if (query == null || query.isEmpty()) return m;
        for (String kv : query.split("&")) {
            int i = kv.indexOf('=');
            if (i < 0) continue;
            String k = java.net.URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8);
            String v = java.net.URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8);
            m.put(k, v);
        }
        return m;
    }
}