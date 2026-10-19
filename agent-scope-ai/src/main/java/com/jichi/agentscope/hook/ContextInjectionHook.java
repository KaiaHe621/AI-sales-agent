package com.jichi.agentscope.hook;

import com.jichi.agentscope.context.UserContextHolder;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PreReasoningEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class ContextInjectionHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreReasoningEvent e) {
            String userContext = UserContextHolder.get();

            if (userContext != null && !userContext.isBlank()) {
                List<Msg> messages = new ArrayList<>(e.getInputMessages());
                messages.add(0, Msg.builder()
                        .role(MsgRole.SYSTEM)
                        .content(List.of(
                                TextBlock.builder()
                                        .text("当前用户上下文：\n" + userContext)
                                        .build()
                        ))
                        .build());
                e.setInputMessages(messages);
            }
        }
        return Mono.just(event);
    }
}