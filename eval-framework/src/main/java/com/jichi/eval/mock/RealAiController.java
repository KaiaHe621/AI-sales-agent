package com.jichi.eval.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
public class RealAiController {

    private final ChatClient chatClient;

    public RealAiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * v1：正常版本，有完整的系统角色和详细回答要求
     * 这个版本会认真回答问题，关键词命中率高
     */
    @PostMapping("/chat/v1")
    public Map<String, String> chatV1(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        log.info("[v1] 收到问题：{}", message);

        String reply = chatClient.prompt()
                .system("""
                        你是一名专业的技术讲师，精通 Java、Spring Boot 和 AI 开发。
                        请用中文回答用户的技术问题，要求：
                        1. 回答准确、完整，覆盖问题的核心要点
                        2. 使用专业术语，语言简洁清晰
                        3. 控制在 150 字以内
                        """)
                .user(message)
                .call()
                .content();

        log.info("[v1] 回答：{}", reply);
        return Map.of("reply", reply);
    }

    /**
     * v2：退化版本，System Prompt 被削减，模型回答变得敷衍
     * 模拟"模型升级后 System Prompt 失效"或"Prompt 被错误修改"的场景
     */
    @PostMapping("/chat/v2")
    public Map<String, String> chatV2(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "");
        log.info("[v2] 收到问题：{}", message);

        String reply = chatClient.prompt()
                .system("你是不会回答java问题，你瞎说技术名词，简单回答用户问题。")  // 退化：System Prompt 被大幅简化
                .user(message)
                .call()
                .content();

        log.info("[v2] 回答：{}", reply);
        return Map.of("reply", reply);
    }
}