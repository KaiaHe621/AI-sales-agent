package com.jichi.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class CurrencyTools {

    @Tool("查询两种货币之间的实时汇率")
    public String getExchangeRate(
            @P("源货币代码，例如 USD、EUR、JPY") String from,
            @P("目标货币代码，例如 CNY、USD") String to) {
        return String.format("{\"from\":\"%s\",\"to\":\"%s\",\"rate\":7.24}", from, to);
    }
}