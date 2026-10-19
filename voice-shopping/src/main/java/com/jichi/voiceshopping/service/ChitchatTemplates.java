package com.jichi.voiceshopping.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ChitchatTemplates {

    private static final Map<String, String> TEMPLATES = Map.of(
            "你是(谁|什么|ai|机器人).*", "我是鸡哥家的语音导购，帮你挑好商品。你想看点啥？",
            ".*(会不会|能不能).*(推荐|挑).*", "当然能！你说个需求我就帮你挑。",
            ".*(天气|冷|热).*", "天气就不聊了哈，我主业是帮你选东西。想看点啥？"
    );

    public Optional<String> match(String text) {
        for (var e : TEMPLATES.entrySet()) {
            if (text.matches(e.getKey())) return Optional.of(e.getValue());
        }
        return Optional.empty();
    }
}