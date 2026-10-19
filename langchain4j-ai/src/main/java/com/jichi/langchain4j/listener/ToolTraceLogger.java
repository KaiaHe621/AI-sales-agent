package com.jichi.langchain4j.listener;

import dev.langchain4j.service.tool.ToolExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@Slf4j
public class ToolTraceLogger implements Consumer<ToolExecution> {

    @Override
    public void accept(ToolExecution execution) {
        // System.out.println 不受日志级别限制，调试必见
        System.out.println(String.format(
                ">>> [ToolTraceLogger] 工具名：%s，参数：%s，结果：%s",
                execution.request().name(),
                execution.request().arguments(),
                execution.result()));
        log.info("[工具追踪] 工具名：{}，参数：{}，结果：{}",
                execution.request().name(),
                execution.request().arguments(),
                execution.result());
    }
}