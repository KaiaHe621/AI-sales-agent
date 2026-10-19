package com.jichi.langchain4j.controller.prompt;

import com.jichi.langchain4j.service.CodeAssistant;
import com.jichi.langchain4j.service.Translator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prompt/user-msg")
public class UserMessageController {

    private final Translator translator;
    private final CodeAssistant codeAssistant;

    public UserMessageController(Translator translator, CodeAssistant codeAssistant) {
        this.translator = translator;
        this.codeAssistant = codeAssistant;
    }

    @GetMapping("/translate")
    public String translate(@RequestParam String language, @RequestParam String text) {
        return translator.translate(language, text);
    }

    @PostMapping("/review")
    public String reviewCode(@RequestBody String code) {
        return codeAssistant.reviewCode(code);
    }
}