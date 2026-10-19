package com.jichi.mcp.client;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpAgentController {

    private final McpClientManager mcpManager;
    private final McpDrivenAgent agent;

    /** 查看当前 Agent 能用哪些工具 */
    @GetMapping("/tools")
    public List<String> listAllTools() {
        return mcpManager.getAllTools().stream()
                .map(t -> t.name() + "：" + t.description())
                .toList();
    }

    /** 和 Agent 对话，Agent 会自动调用 MCP 工具 */
    @PostMapping("/chat")
    public String chat(@RequestBody Map<String, String> body) {
        return agent.chat(body.get("message"));
    }
}