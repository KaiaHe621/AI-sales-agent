package com.jichi.langchain4j.service.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class ArithmeticTools {

    @Tool("计算两个数字的加法")
    public double add(@P("第一个数字") double a, @P("第二个数字") double b) {
        System.out.println(">>> [ArithmeticTools] add 被调用: " + a + " + " + b);
        return a + b;
    }

    @Tool("计算两个数字的乘法")
    public double multiply(@P("第一个数字") double a, @P("第二个数字") double b) {
        System.out.println(">>> [ArithmeticTools] multiply 被调用: " + a + " × " + b);
        return a * b;
    }
}