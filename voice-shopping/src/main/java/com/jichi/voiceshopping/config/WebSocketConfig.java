package com.jichi.voiceshopping.config;

import com.jichi.voiceshopping.controller.VoiceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoiceWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/voice")
                .addInterceptors(new VoiceHandshakeInterceptor())
                .setAllowedOriginPatterns("*");
    }
}