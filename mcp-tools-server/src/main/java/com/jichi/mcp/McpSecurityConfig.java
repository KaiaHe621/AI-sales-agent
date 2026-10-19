package com.jichi.mcp;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpSecurityConfig {

    @Bean
    public FilterRegistrationBean<McpApiKeyFilter> mcpApiKeyFilter() {
        String apiKey = System.getenv("MCP_API_KEY");
        if (apiKey == null) {
            apiKey = "jichiTest";
        }

        FilterRegistrationBean<McpApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new McpApiKeyFilter(apiKey));
        registration.addUrlPatterns("/sse", "/mcp/*");   // 只拦截 MCP 端点
        registration.setOrder(1);
        registration.setName("mcpApiKeyFilter");
        return registration;
    }
}