package com.jichi.langchain4j.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

@Component
public class CalculatorTools {

    @Tool("计算数学表达式，支持加减乘除和括号，返回计算结果")
    public String calculate(@P("数学表达式，例如：168.42 * 7.24") String expression) {
        try {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            return String.valueOf(result);
        } catch (Exception e) {
            return "计算错误：" + e.getMessage();
        }
    }
}