package com.jichi.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpResourcesConfig {

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> staticResources() {
        return List.of(
                new McpServerFeatures.SyncResourceSpecification(
                        new McpSchema.Resource(
                                "docs://handbook/onboarding",
                                "员工入职手册",
                                "公司员工入职流程和规范文档",
                                "text/plain",
                                null
                        ),
                        (exchange, resourceRequest) -> readStaticResource(resourceRequest)
                ),
                new McpServerFeatures.SyncResourceSpecification(
                        new McpSchema.Resource(
                                "docs://api/overview",
                                "API 接口文档总览",
                                "后端 API 接口清单和说明",
                                "text/markdown",
                                null
                        ),
                        (exchange, resourceRequest) -> readStaticResource(resourceRequest)
                ),
                new McpServerFeatures.SyncResourceSpecification(
                        new McpSchema.Resource(
                                "config://app/production",
                                "生产环境配置",
                                "应用生产环境的配置参数（脱敏版）",
                                "application/json",
                                null
                        ),
                        (exchange, resourceRequest) -> readStaticResource(resourceRequest)
                )
        );
    }

    private McpSchema.ReadResourceResult readStaticResource(McpSchema.ReadResourceRequest resourceRequest) {
        String uri = resourceRequest.uri();
        String content = switch (uri) {
            case "docs://handbook/onboarding" -> readOnboardingDoc();
            case "docs://api/overview"        -> readApiDoc();
            case "config://app/production"    -> readProductionConfig();
            default -> "资源不存在：" + uri;
        };

        return new McpSchema.ReadResourceResult(
                List.of(new McpSchema.TextResourceContents(uri, "text/plain", content))
        );
    }

    private String readOnboardingDoc() {
        // 实际可以从文件、数据库、CMS 读取，这里用硬编码演示
        return """
                # 员工入职手册
                
                ## 入职第一天
                1. 领取工牌和电脑
                2. 配置 VPN 和开发环境
                3. 阅读代码规范文档
                
                ## 常用系统
                - OA 系统：https://oa.company.com
                - 代码仓库：https://git.company.com
                - 知识库：https://wiki.company.com
                """;
    }

    private String readApiDoc() {
        return """
                # API 接口总览
                
                ## 订单模块
                - GET /api/orders/{id} 查询订单详情
                - POST /api/orders 创建订单
                - PUT /api/orders/{id}/cancel 取消订单
                
                ## 用户模块
                - GET /api/users/{id} 查询用户信息
                """;
    }

    private String readProductionConfig() {
        return """
                {
                  "app.timeout": 30,
                  "cache.ttl": 3600,
                  "rate.limit": 100,
                  "feature.new_checkout": true
                }
                """;
    }
}