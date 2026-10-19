package com.jichi.agentscope.config;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SqlPipelineConfig {

    @Bean("sqlQualityAgent")
    public SequentialAgent sqlQualityAgent(Model model) {

        // 步骤一：SQL 生成器
        ReActAgent.Builder sqlGenBuilder = ReActAgent.builder()
                .name("sql_generator")
                .model(model)
                .sysPrompt("""
                        你是一个 MySQL 专家。
                        根据用户的自然语言描述，输出对应的 SQL 语句。
                        只输出 SQL，不要解释。
                        """)
                .memory(new InMemoryMemory());

        AgentScopeAgent sqlGenerator = AgentScopeAgent.fromBuilder(sqlGenBuilder)
                .name("sql_generator")
                .description("将自然语言转换为 MySQL SQL")
                .instruction("{input}")             // 接收原始用户输入
                .includeContents(false)
                .outputKey("sql")                   // 输出存入 "sql" 键
                .build();

        // 步骤二：SQL 评分器（接收步骤一的 sql + 原始 input）
        ReActAgent.Builder sqlRaterBuilder = ReActAgent.builder()
                .name("sql_rater")
                .model(model)
                .sysPrompt("""
                        你是一个 SQL 质量评审员。
                        根据用户的原始需求和生成的 SQL，输出一个 0 到 1 之间的质量分。
                        只输出数字，不要解释。例如：0.85
                        """)
                .memory(new InMemoryMemory());

        AgentScopeAgent sqlRater = AgentScopeAgent.fromBuilder(sqlRaterBuilder)
                .name("sql_rater")
                .description("评估 SQL 与用户意图的匹配度")
                .instruction("生成的 SQL 是：\n{sql}\n\n原始需求是：\n{input}")  // 引用上游的 sql 键
                .includeContents(false)
                .outputKey("score")                 // 输出存入 "score" 键
                .build();

        // 组装顺序 Pipeline
        return SequentialAgent.builder()
                .name("sql_quality_pipeline")
                .description("自然语言转 SQL，并评估质量")
                .subAgents(List.of(sqlGenerator, sqlRater))
                .build();
    }
}