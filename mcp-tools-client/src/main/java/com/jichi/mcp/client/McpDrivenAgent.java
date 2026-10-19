package com.jichi.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class McpDrivenAgent {

    private final ChatClient chatClient;
    private final McpClientManager mcpManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public McpDrivenAgent(ChatClient.Builder builder, McpClientManager mcpManager) {
        this.mcpManager = mcpManager;

        List<ToolCallback> mcpToolCallbacks = buildMcpToolCallbacks();
        log.info("[Agent] 从 MCP Server 加载了 {} 个工具", mcpToolCallbacks.size());

        this.chatClient = builder
                .defaultSystem("""
                        你是一个智能助手，可以访问文件系统和 GitHub。
                        工具名格式：serverName__toolName，例如 filesystem__read_file。
                        需要操作文件时用 filesystem 系列工具，需要查 GitHub 时用 github 系列工具。
                        """)
                .defaultToolCallbacks(mcpToolCallbacks.toArray(new ToolCallback[0]))
                .build();
    }

    private List<ToolCallback> buildMcpToolCallbacks() {
        return mcpManager.getAllTools().stream()
                .map(tool -> (ToolCallback) new ToolCallback() {
                    @Override
                    public ToolDefinition getToolDefinition() {
                        String schemaJson;
                        try {
                            // inputSchema() 返回 JsonSchema 对象，必须序列化成 JSON 字符串
                            schemaJson = objectMapper.writeValueAsString(tool.inputSchema());
                        } catch (Exception e) {
                            schemaJson = "{}";
                        }
                        return ToolDefinition.builder()
                                .name(tool.name())
                                .description(tool.description())
                                .inputSchema(schemaJson)
                                .build();
                    }

                    @Override
                    public String call(String toolInput) {
                        try {
                            Map<String, Object> args = objectMapper.readValue(toolInput, Map.class);
                            return mcpManager.callTool(tool.name(), args);
                        } catch (Exception e) {
                            log.error("[Agent] 工具 {} 调用失败：{}", tool.name(), e.getMessage());
                            return "工具调用失败：" + e.getMessage();
                        }
                    }
                })
                .toList();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
