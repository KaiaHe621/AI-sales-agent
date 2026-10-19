package com.jichi.langchain4j.config;

import com.jichi.langchain4j.service.tools.FinanceAssistant;
import com.jichi.langchain4j.tools.CalculatorTools;
import com.jichi.langchain4j.tools.CurrencyTools;
import com.jichi.langchain4j.tools.StockTools;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FinanceAssistantConfig {

    @Bean
    @Primary
    public FinanceAssistant financeAssistantWithTools(ChatModel model,
                                                      StockTools stockTools,
                                                      CurrencyTools currencyTools,
                                                      CalculatorTools calculatorTools) {
        return AiServices.builder(FinanceAssistant.class)
                .chatModel(model)
                .tools(stockTools, currencyTools, calculatorTools)
                .build();
    }
}