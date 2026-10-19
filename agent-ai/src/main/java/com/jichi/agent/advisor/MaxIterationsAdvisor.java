package com.jichi.agent.advisor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用 Spring AOP 拦截所有 @Tool 方法，统计当前请求的工具调用次数。
 * 比实现 Spring AI 内部 Advisor 接口更稳定，不依赖框架私有 API。
 */
@Aspect
@Component
public class MaxIterationsAdvisor {

    private static final int DEFAULT_MAX_ITERATIONS = 10;

    // ThreadLocal 保证同一请求内的计数互不干扰
    private static final ThreadLocal<AtomicInteger> CALL_COUNT =
            ThreadLocal.withInitial(AtomicInteger::new);

    /**
     * 每次新 Agent 任务开始前调用，重置计数器
     */
    public static void reset() {
        CALL_COUNT.remove();
    }

    /**
     * 拦截项目内所有标注了 @Tool 的方法
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object limitToolCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        int count = CALL_COUNT.get().incrementAndGet();

        if (count > DEFAULT_MAX_ITERATIONS) {
            CALL_COUNT.remove();
            throw new AgentMaxIterationsException(
                    "Agent 超过最大工具调用次数（" + DEFAULT_MAX_ITERATIONS + "），任务中止");
        }

        return joinPoint.proceed();
    }
}