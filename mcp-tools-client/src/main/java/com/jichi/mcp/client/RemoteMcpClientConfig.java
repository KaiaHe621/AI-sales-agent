
package com.jichi.mcp.client;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RemoteMcpClientConfig {

    @Bean
    public McpSyncClient remoteToolsClient() {
        // SSE 传输层：只需要传 Server 的 URL

        //String apiKey = System.getenv("MCP_API_KEY");
        String apiKey = "jichiTest";
        HttpClientSseClientTransport.Builder transportBuilder =
                HttpClientSseClientTransport.builder("http://localhost:8090");
        if (apiKey != null) {
            transportBuilder.customizeRequest(builder -> builder.header("X-API-Key", apiKey));
        }
        HttpClientSseClientTransport transport = transportBuilder.build();
        McpSyncClient client = McpClient.sync(transport)
                .clientInfo(new McpSchema.Implementation("jichi-agent", "1.0.0"))
                .build();

        // initialize() 内部自动完成握手通知，不需要额外调用
        McpSchema.InitializeResult result = client.initialize();
        log.info("[MCP] 已连接远程 Server：{} v{}",
                result.serverInfo().name(), result.serverInfo().version());

        return client;
    }
}