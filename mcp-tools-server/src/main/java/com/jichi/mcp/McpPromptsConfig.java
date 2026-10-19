package com.jichi.mcp;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class McpPromptsConfig {

    // SyncPromptRegistrationCallback 已在新版 Spring AI MCP 中移除
    // 改用 List<SyncPromptSpecification>，每个 Prompt 自带独立的 handler，和 Resources 模式一致
    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> prompts() {
        return List.of(
                new McpServerFeatures.SyncPromptSpecification(
                        new McpSchema.Prompt(
                                "code_review",
                                "代码审查模板",
                                "对 Java 代码进行全面的审查，包括代码质量、安全性、性能等维度",
                                List.of(
                                        new McpSchema.PromptArgument("code", "要审查的 Java 代码", true),
                                        new McpSchema.PromptArgument("focus",
                                                "重点关注方向：security/performance/readability/all，默认 all", false)
                                )
                        ),
                        (exchange, request) -> buildCodeReviewPrompt(request.arguments())
                ),
                new McpServerFeatures.SyncPromptSpecification(
                        new McpSchema.Prompt(
                                "sales_report",
                                "销售分析报告模板",
                                "基于销售数据生成结构化的分析报告，适合汇报给管理层",
                                List.of(
                                        new McpSchema.PromptArgument("data", "销售数据（文字描述或结构化数据）", true),
                                        new McpSchema.PromptArgument("period", "报告周期，例如：上周、上月、Q1", true),
                                        new McpSchema.PromptArgument("audience",
                                                "报告受众：executive/team/client，默认 team", false)
                                )
                        ),
                        (exchange, request) -> buildSalesReportPrompt(request.arguments())
                ),
                new McpServerFeatures.SyncPromptSpecification(
                        new McpSchema.Prompt(
                                "sql_generator",
                                "自然语言转 SQL 模板",
                                "根据自然语言描述生成对应的 SQL 查询语句",
                                List.of(
                                        new McpSchema.PromptArgument("question", "查询需求的自然语言描述", true),
                                        new McpSchema.PromptArgument("schema",
                                                "数据库表结构（DDL 或描述）", true),
                                        new McpSchema.PromptArgument("dialect",
                                                "SQL 方言：mysql/postgresql/h2，默认 mysql", false)
                                )
                        ),
                        (exchange, request) -> buildSqlGeneratorPrompt(request.arguments())
                ),
                new McpServerFeatures.SyncPromptSpecification(
                        new McpSchema.Prompt(
                                "customer_reply",
                                "客服回复话术模板",
                                "生成专业、友善的客服回复，适合电商客服场景",
                                List.of(
                                        new McpSchema.PromptArgument("issue", "用户反映的问题", true),
                                        new McpSchema.PromptArgument("context",
                                                "相关背景信息（订单状态、历史记录等）", false),
                                        new McpSchema.PromptArgument("tone",
                                                "语气风格：formal/friendly/apologetic，默认 friendly", false)
                                )
                        ),
                        (exchange, request) -> buildCustomerReplyPrompt(request.arguments())
                )
        );
    }

    // -------- 提示词内容构建 --------

    private McpSchema.GetPromptResult buildCodeReviewPrompt(Map<String, Object> args) {
        String code = String.valueOf(args.getOrDefault("code", ""));
        String focus = String.valueOf(args.getOrDefault("focus", "all"));

        String focusInstruction = switch (focus) {
            case "security"     -> "重点审查安全漏洞：SQL 注入、XSS、权限校验缺失、敏感信息泄露等。";
            case "performance"  -> "重点审查性能问题：N+1 查询、不必要的循环、内存泄漏风险、线程安全等。";
            case "readability"  -> "重点审查代码可读性：命名规范、注释质量、方法长度、职责单一等。";
            default             -> "全面审查：代码质量、安全性、性能、可读性、测试覆盖度。";
        };

        String systemPrompt = """
                你是一名资深 Java 开发工程师，有丰富的代码审查经验。
                审查要有理有据，指出具体问题位置，并给出改进建议和示例代码。
                发现严重问题时明确标注「严重」，一般建议标注「建议」。
                """;

        String userPrompt = focusInstruction + "\n\n请审查以下代码：\n\n```java\n" + code + "\n```";

        return new McpSchema.GetPromptResult(
                "Java 代码审查",
                List.of(
                        new McpSchema.PromptMessage(
                                McpSchema.Role.USER,
                                new McpSchema.TextContent(systemPrompt + "\n\n" + userPrompt)
                        )
                )
        );
    }

    private McpSchema.GetPromptResult buildSalesReportPrompt(Map<String, Object> args) {
        String data = String.valueOf(args.getOrDefault("data", ""));
        String period = String.valueOf(args.getOrDefault("period", ""));
        String audience = String.valueOf(args.getOrDefault("audience", "team"));

        String style = switch (audience) {
            case "executive" -> "简洁、重点突出，不超过 300 字，包含核心指标和关键结论";
            case "client"    -> "专业、正式，突出正向数据，措辞谨慎";
            default          -> "详细、有数据支撑，包含问题分析和改进建议";
        };

        String prompt = String.format("""
                请根据以下销售数据生成 %s 的销售分析报告。
                
                报告风格要求：%s
                
                报告结构：
                1. 核心指标（销售额、订单量、客单价）
                2. 对比分析（和上期对比的变化及原因）
                3. 异常和亮点
                4. 改进建议（如果适用）
                
                销售数据：
                %s
                """, period, style, data);

        return new McpSchema.GetPromptResult(
                period + "销售分析报告",
                List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER,
                        new McpSchema.TextContent(prompt)))
        );
    }

    private McpSchema.GetPromptResult buildSqlGeneratorPrompt(Map<String, Object> args) {
        String question = String.valueOf(args.getOrDefault("question", ""));
        String schema = String.valueOf(args.getOrDefault("schema", ""));
        String dialect = String.valueOf(args.getOrDefault("dialect", "mysql"));

        String prompt = String.format("""
                你是一个 SQL 专家，擅长 %s。
                根据以下数据库结构和查询需求，生成准确的 SQL 语句。
                
                要求：
                - 只输出 SQL，不要解释
                - SQL 需要可直接执行
                - 涉及大表时加合适的 WHERE 条件和 LIMIT
                
                数据库结构：
                %s
                
                查询需求：%s
                """, dialect.toUpperCase(), schema, question);

        return new McpSchema.GetPromptResult(
                "SQL 查询语句",
                List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER,
                        new McpSchema.TextContent(prompt)))
        );
    }

    private McpSchema.GetPromptResult buildCustomerReplyPrompt(Map<String, Object> args) {
        String issue = String.valueOf(args.getOrDefault("issue", ""));
        String context = String.valueOf(args.getOrDefault("context", ""));
        String tone = String.valueOf(args.getOrDefault("tone", "friendly"));

        String toneInstruction = switch (tone) {
            case "formal"     -> "正式、专业，保持商务语气";
            case "apologetic" -> "诚恳致歉，展现责任担当，安抚情绪";
            default           -> "友善、亲切，像和朋友对话一样自然";
        };

        String prompt = String.format("""
                你是一名专业的电商客服，请用以下语气风格回复用户：%s
                
                回复要求：
                - 直接解决用户问题，不说废话
                - 如果需要用户提供信息，明确说明需要什么
                - 如果问题无法立即解决，给出明确的时间预期
                - 字数控制在 150 字以内
                
                用户问题：%s
                
                %s
                """,
                toneInstruction, issue,
                context.isBlank() ? "" : "背景信息：\n" + context);

        return new McpSchema.GetPromptResult(
                "客服回复",
                List.of(new McpSchema.PromptMessage(
                        McpSchema.Role.USER,
                        new McpSchema.TextContent(prompt)))
        );
    }
}