package com.jichi.agentscope.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreActingEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.message.TextBlock;
import reactor.core.publisher.Mono;

public class ToolMonitorHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreActingEvent e) {
            System.out.println("[工具调用] 工具名：" + e.getToolUse().getName());
            System.out.println("[工具调用] 参数：" + e.getToolUse().getInput());
        }

        if (event instanceof PostActingEvent e) {
            String result = e.getToolResult().getOutput().stream()
                    .filter(block -> block instanceof TextBlock)
                    .map(block -> ((TextBlock) block).getText())
                    .findFirst()
                    .orElse("");
            System.out.println("[工具结果] " + result);
        }

        return Mono.just(event);  // 原样返回，不修改
    }
}