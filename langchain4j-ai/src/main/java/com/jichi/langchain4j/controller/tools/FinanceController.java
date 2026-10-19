package com.jichi.langchain4j.controller.tools;

import com.jichi.langchain4j.service.tools.FinanceAssistant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tool/finance")
public class FinanceController {

    private final FinanceAssistant financeAssistant;

    public FinanceController(FinanceAssistant financeAssistant) {
        this.financeAssistant = financeAssistant;
    }

    @GetMapping
    public String chat(@RequestParam String message) {
        return financeAssistant.chat(message);
    }
}