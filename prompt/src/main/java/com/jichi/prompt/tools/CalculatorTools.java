package com.jichi.prompt.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools {

    @Tool(description = "计算数学表达式")
    public String calculate(
            @ToolParam(description = "要计算的数学表达式，例如：(10 + 5) * 3 / 2") String expression) {
        return expression + " = 计算结果（实际应接入表达式求值库）";
    }
}