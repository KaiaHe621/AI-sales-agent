package com.jichi.langchain4j.aspect;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@Slf4j
public class ToolExceptionAspect {

    // 拦截所有加了 @Tool 注解的方法
    @Around("@annotation(dev.langchain4j.agent.tool.Tool)")
    public Object handleToolException(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String toolName = method.getAnnotation(Tool.class).value()[0];

        try {
            return joinPoint.proceed();
        } catch (IllegalArgumentException e) {
            // 参数不合法，返回提示让模型修正参数重试
            log.warn("[Tool] 参数不合法，工具：{}，原因：{}", toolName, e.getMessage());
            return "参数不合法：" + e.getMessage() + "，请检查后重试";
        } catch (Exception e) {
            // 其他异常，记录完整日志但只返回友好提示
            log.error("[Tool] 执行失败，工具：{}，异常：{}", toolName, e.getMessage(), e);
            return "工具执行失败，请稍后重试或联系人工客服";
        }
    }
}