package com.jichi.agentscope.controller;

import com.jichi.agentscope.tool.ProgressCallback;
import com.jichi.agentscope.tool.ReportGeneratorTool;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolExecutionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class ReportController {

    private final DashScopeChatModel model;
    private final ReportGeneratorTool reportTool;

    /**
     * GET /report/stream?dataRange=最近30天
     * 响应类型为 text/event-stream，客户端实时接收进度
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReport(
            @RequestParam(defaultValue = "最近30天") String dataRange) {

        SseEmitter sseEmitter = new SseEmitter(120_000L);

        // Agent 的阻塞调用放到独立线程，避免占用 Tomcat 请求线程
        CompletableFuture.runAsync(() -> {
            try {
                // 把 SseEmitter 包装成 ProgressCallback，每次 report() 立即推送 SSE 事件
                ProgressCallback callback = message -> {
                    try {
                        sseEmitter.send(SseEmitter.event().name("progress").data(message));
                    } catch (IOException ex) {
                        // 客户端已断开，忽略
                    }
                };

                // 注册到 ToolExecutionContext，工具方法中 ProgressCallback 参数会自动注入
                ToolExecutionContext context = ToolExecutionContext.builder()
                        .register(callback)
                        .build();

                Toolkit toolkit = new Toolkit();
                toolkit.registerTool(reportTool);

                ReActAgent agent = ReActAgent.builder()
                        .name("报告助手")
                        .model(model)
                        .sysPrompt("你可以生成数据分析报告。")
                        .toolkit(toolkit)
                        .toolExecutionContext(context)
                        .build();

                Msg response = agent.call(
                        Msg.builder()
                                .textContent("帮我生成" + dataRange + "的销售分析报告")
                                .build()
                ).block();

                // Agent 最终回复作为 final 事件推出
                sseEmitter.send(SseEmitter.event().name("final").data(response.getTextContent()));
                sseEmitter.complete();

            } catch (Exception e) {
                sseEmitter.completeWithError(e);
            }
        });

        return sseEmitter;
    }
}