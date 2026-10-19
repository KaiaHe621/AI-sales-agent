package com.jichi.agentscope.hook;

import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreCallEvent;
import io.agentscope.core.hook.PostCallEvent;
import reactor.core.publisher.Mono;

public class LoggingHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        // 用 instanceof 模式匹配处理不同事件
        if (event instanceof PreCallEvent e) {
            System.out.println("[Hook] Agent " + e.getAgent().getName()
                    + " 开始处理消息");
        } else if (event instanceof PostCallEvent e) {
            System.out.println("[Hook] Agent " + e.getAgent().getName()
                    + " 处理完成：" + e.getFinalMessage().getTextContent());
        }
        // 必须返回 Mono.just(event)，否则会中断执行链
        return Mono.just(event);
    }
}