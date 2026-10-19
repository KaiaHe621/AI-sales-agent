package com.jichi.prompt.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/code-gen")
public class GenerateAndVerifyController {

    private final DashScopeChatModel chatModel;

    public GenerateAndVerifyController(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/generate-and-verify")
    public String generateAndVerify(@RequestBody String requirement) {
        String code = chatModel.call(new Prompt(
                new UserMessage(requirement)
        )).getResult().getOutput().getText();

        String reviewResult = chatModel.call(new Prompt(
                new UserMessage(String.format("""
                        你刚才生成了以下代码，请再次检查是否有编译错误、运行时异常风险或明显的 bug：
                        
                        ```java
                        %s
                        ```
                        
                        如果有问题，直接给出修正后的完整代码。
                        如果没有问题，回复"代码无误"。
                        """, code))
        )).getResult().getOutput().getText();

        if (reviewResult.contains("代码无误")) {
            return code;
        } else {
            return reviewResult;
        }
    }
}