package com.jichi.mcp;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class McpApiKeyFilter implements Filter {

    private final String validApiKey;

    public McpApiKeyFilter(String validApiKey) {
        this.validApiKey = validApiKey;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String apiKey = httpReq.getHeader("X-API-Key");

        if (!validApiKey.equals(apiKey)) {
            log.warn("[MCP] 非法访问，来自 {}", httpReq.getRemoteAddr());
            ((HttpServletResponse) response).sendError(401, "Invalid API Key");
            return;
        }

        chain.doFilter(request, response);
    }
}