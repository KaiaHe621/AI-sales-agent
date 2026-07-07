package com.jichi.salesAgent.config;

import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TokenUsageLogger implements ChatModelListener {

    private final Counter inputTokenCounter;
    private final Counter outputTokenCounter;

    public TokenUsageLogger(MeterRegistry meterRegistry) {
        this.inputTokenCounter = Counter.builder("llm.tokens.input")
                .description("Input tokens consumed")
                .register(meterRegistry);
        this.outputTokenCounter = Counter.builder("llm.tokens.output")
                .description("Output tokens consumed")
                .register(meterRegistry);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        var usage = responseContext.chatResponse().tokenUsage();
        if (usage != null) {
            int input = usage.inputTokenCount() != null ? usage.inputTokenCount() : 0;
            int output = usage.outputTokenCount() != null ? usage.outputTokenCount() : 0;

            inputTokenCounter.increment(input);
            outputTokenCounter.increment(output);

            // 估算费用（qwen-max 价格：输入 0.04 元/千Token，输出 0.12 元/千Token）
            double cost = input * 0.04 / 1000.0 + output * 0.12 / 1000.0;
            log.info("Token 用量 | 输入：{} | 输出：{} | 本次费用约：¥{}",
                    input, output, String.format("%.4f", cost));
        }
    }
}