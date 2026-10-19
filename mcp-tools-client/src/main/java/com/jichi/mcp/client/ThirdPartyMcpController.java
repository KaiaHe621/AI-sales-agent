package com.jichi.mcp.client;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/third-party")
@RequiredArgsConstructor
public class ThirdPartyMcpController {

    private final McpSyncClient filesystemMcpClient;
    private final McpSyncClient githubMcpClient;

    /** 查看 filesystem server 提供的所有工具 */
    @GetMapping("/filesystem/tools")
    public List<String> filesystemTools() {
        return filesystemMcpClient.listTools().tools().stream()
                .map(t -> t.name() + "：" + t.description())
                .toList();
    }

    /** 调用 filesystem 工具，toolName 如 list_directory、read_file */
    @PostMapping("/filesystem/call")
    public String callFilesystem(
            @RequestParam String toolName,
            @RequestBody Map<String, Object> args) {
        McpSchema.CallToolResult result = filesystemMcpClient.callTool(
                new McpSchema.CallToolRequest(toolName, args));
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst().orElse("（无返回内容）");
    }

    /** 查看 GitHub server 提供的所有工具 */
    @GetMapping("/github/tools")
    public List<String> githubTools() {
        return githubMcpClient.listTools().tools().stream()
                .map(t -> t.name() + "：" + t.description())
                .toList();
    }

    /** 调用 GitHub 工具，toolName 如 search_repositories、get_file_contents */
    @PostMapping("/github/call")
    public String callGithub(
            @RequestParam String toolName,
            @RequestBody Map<String, Object> args) {
        McpSchema.CallToolResult result = githubMcpClient.callTool(
                new McpSchema.CallToolRequest(toolName, args));
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst().orElse("（无返回内容）");
    }
}
