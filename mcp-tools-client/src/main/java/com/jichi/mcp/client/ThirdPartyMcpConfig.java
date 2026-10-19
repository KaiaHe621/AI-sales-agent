package com.jichi.mcp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class ThirdPartyMcpConfig {

    private final ObjectMapper objectMapper;

    /**
     * 连接官方文件系统 MCP Server
     * 能力：读写本地文件、列目录
     * 第一次运行时 npx 会自动下载包，稍等几秒
     */
    //@Bean(name = "filesystemMcpClient")
    public McpSyncClient filesystemClient() {
        // Java ProcessBuilder 不继承 Shell 的 PATH，必须用绝对路径
        // which npx 查路径，Homebrew 安装通常是 /opt/homebrew/bin/npx
        String npx = "/Users/yourname/.nvm/versions/node/v20.19.6/bin/npx";

        StdioClientTransport transport = new StdioClientTransport(
                ServerParameters.builder(npx)
                        .args("-y", "@modelcontextprotocol/server-filesystem",
                                "/Users/yourname/Documents/code/jc-ai/mcp-tools-server")
                        .build(),
                new JacksonMcpJsonMapper(objectMapper)
        );

        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("jichi-agent", "1.0.0"))
                .build();

        client.initialize();

        log.info("[MCP] 已连接文件系统 Server，工具数：{}",
                client.listTools().tools().size());
        return client;
    }

    /**
     * 连接官方 GitHub MCP Server
     * 能力：搜索仓库、读文件、查 PR、查 Issue
     * 需要环境变量 GITHUB_TOKEN（Personal Access Token）
     */
    //@Bean(name = "githubMcpClient")
    public McpSyncClient githubClient() {
        String npx = "/Users/yourname/.nvm/versions/node/v20.19.6/bin/npx";

        StdioClientTransport transport = new StdioClientTransport(
                ServerParameters.builder(npx)
                        .args("-y", "@modelcontextprotocol/server-github")
                        .env(Map.of("GITHUB_PERSONAL_ACCESS_TOKEN",
                                System.getenv("GITHUB_TOKEN")))   // 从环境变量读，不要硬编码到代码里
                        .build(),
                new JacksonMcpJsonMapper(objectMapper)
        );

        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("jichi-agent", "1.0.0"))
                .build();

        client.initialize();

        log.info("[MCP] 已连接 GitHub Server，工具数：{}",
                client.listTools().tools().size());
        return client;
    }
}