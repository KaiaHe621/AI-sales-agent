package com.jichi.mcp.client;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class McpClientManager {

    private final Map<String, McpSyncClient> clients;

    public McpClientManager(
            McpSyncClient filesystemMcpClient,
            McpSyncClient githubMcpClient) {

        this.clients = Map.of(
                "filesystem", filesystemMcpClient,
                "github",     githubMcpClient
        );
    }

    /**
     * 获取所有 Server 的工具列表，合并后供 Agent 使用
     * 工具名加上 serverName 前缀，避免不同 Server 的同名工具冲突
     */
    public List<McpSchema.Tool> getAllTools() {
        List<McpSchema.Tool> allTools = new ArrayList<>();
        clients.forEach((serverName, client) -> {
            try {
                client.listTools().tools().forEach(tool ->
                        // 用双下划线 __ 分隔 serverName 和 toolName
                        // OpenAI/DeepSeek 工具名只允许 ^[a-zA-Z0-9_-]+$，点号不合法
                        allTools.add(McpSchema.Tool.builder()
                                .name(serverName + "__" + tool.name())
                                .description(tool.description())
                                .inputSchema(tool.inputSchema())
                                .build())
                );
            } catch (Exception e) {
                log.error("[MCP] {} 工具列表获取失败：{}", serverName, e.getMessage());
            }
        });
        return allTools;
    }

    /**
     * 按工具名路由到正确的 Server 执行
     * 格式：serverName.toolName，例如 filesystem.read_file
     */
    public String callTool(String qualifiedToolName, Map<String, Object> arguments) {
        String[] parts = qualifiedToolName.split("__", 2);
        if (parts.length != 2) {
            return "工具名格式错误，应为 serverName__toolName，例如 filesystem__read_file";
        }

        String serverName = parts[0];
        String toolName   = parts[1];

        McpSyncClient client = clients.get(serverName);
        if (client == null) {
            return "未找到 Server：" + serverName + "，可用 Server：" + clients.keySet();
        }

        McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest(toolName, arguments));

        if (Boolean.TRUE.equals(result.isError())) {
            log.warn("[MCP] {}__{}  执行失败", serverName, toolName);
            return "工具执行失败";
        }

        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst()
                .orElse("（无返回内容）");
    }
}