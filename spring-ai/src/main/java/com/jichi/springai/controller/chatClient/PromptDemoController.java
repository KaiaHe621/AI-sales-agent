package com.jichi.springai.controller.chatClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/prompt-demo")
public class PromptDemoController {

    private final ChatClient chatClient;

    public PromptDemoController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    /**
     * 方式一：只有 user 消息
     * 没有任何约束，模型自由发挥
     * GET /prompt-demo/simple?message=JVM
     * 模型可能解释概念，也可能出题，行为不可控
     */
    @GetMapping("/simple")
    public String simple(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 方式二：user + system 消息
     * system 固定了模型角色，输出风格稳定
     * GET /prompt-demo/with-system?message=JVM
     * 模型一定会用提问方式来考你，不会跑偏
     */
    @GetMapping("/with-system")
    public String withSystem(@RequestParam String message) {
        return chatClient.prompt()
                .system("你是一个面试官，用提问的方式检验候选人对知识的掌握程度。只出题，不给答案。")
                .user(message)
                .call()
                .content();
    }

    /**
     * 方式三：动态模板变量
     * 同一套 Prompt 模板，通过参数控制输出方向
     * GET /prompt-demo/template?topic=JVM&difficulty=初级
     * GET /prompt-demo/template?topic=Redis&difficulty=高级
     * 一个接口覆盖所有主题和难度组合
     */
    @GetMapping("/template")
    public String template(
            @RequestParam String topic,
            @RequestParam(defaultValue = "中级") String difficulty) {
        return chatClient.prompt()
                .user(u -> u.text("请出一道关于 {topic} 的 {difficulty} 难度 Java 面试题，只出题，不给答案。")
                            .param("topic", topic)
                            .param("difficulty", difficulty))
                .call()
                .content();
    }
}