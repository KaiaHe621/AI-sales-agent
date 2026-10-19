package com.jichi.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

//@Service
@Slf4j
@RequiredArgsConstructor
public class LocalMcpClientService {

    private final ObjectMapper objectMapper;   // Spring 自动装配的 Jackson ObjectMapper
    private McpSyncClient client;

    @PostConstruct
    public void connect() {
        // StdioClientTransport：Client 直接启动 Server 子进程，通过 stdin/stdout 通信
        // 和 Cursor 接 MCP 的原理完全一样，只是这里 Java 代码扮演了 Cursor 的角色
        StdioClientTransport transport = new StdioClientTransport(
                ServerParameters.builder("java")
                        .args("-jar", "/Users/yourname/Documents/code/jc-ai/mcp-tools-server/target/mcp-tools-server-1.0.1.jar")
                        .build(),
                new JacksonMcpJsonMapper(objectMapper)
        );

        client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("jichi-agent", "1.0.0"))
                .build();

        // 第一步：握手，拿到 Server 的名字和版本
        McpSchema.InitializeResult initResult = client.initialize();
        log.info("[MCP] 已连接 Server：{} v{}",
                initResult.serverInfo().name(), initResult.serverInfo().version());

        // initialize() 内部已自动发送 initialized 通知，无需手动调用
    }

    /**
     * 列出 Server 提供的所有工具
     */
    public List<McpSchema.Tool> listTools() {
        McpSchema.ListToolsResult result = client.listTools();
        return result.tools();
    }

    /**
     * 调用指定工具
     */
    public String callTool(String toolName, Map<String, Object> arguments) {
        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest(toolName, arguments));

        // isError=true 表示工具执行出错，文字内容是错误信息
        if (Boolean.TRUE.equals(result.isError())) {
            log.warn("[MCP] 工具 {} 执行失败", toolName);
            return "工具调用失败";
        }

        // 提取文字内容（content 是数组，可能包含文字、图片等多种类型）
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst()
                .orElse("");
    }

    /**
     * 读取资源（需要 Server 实现了对应的 Resource）
     */
    public String readResource(String uri) {
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest(uri));

        return result.contents().stream()
                .filter(c -> c instanceof McpSchema.TextResourceContents)
                .map(c -> ((McpSchema.TextResourceContents) c).text())
                .findFirst()
                .orElse("");
    }

    @PreDestroy
    public void disconnect() {
        if (client != null) {
            client.close();
            log.info("[MCP] 已断开本地 Server 连接");
        }
    }
}