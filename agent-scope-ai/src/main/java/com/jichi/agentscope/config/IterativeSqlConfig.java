package com.jichi.agentscope.config;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.LoopAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.SequentialAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.loop.LoopMode;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class IterativeSqlConfig {

    private static final double QUALITY_THRESHOLD = 0.8;

    @Bean("iterativeSqlAgent")
    public LoopAgent iterativeSqlAgent(Model model) {

        // 内层是顺序 Pipeline：SQL生成 → SQL评分
        SequentialAgent innerPipeline = buildSqlQualityPipeline(model);

        // 外层 LoopAgent：条件不满足就继续循环
        return LoopAgent.builder()
                .name("iterative_sql_refinement")
                .description("迭代优化 SQL，直到质量分超过 " + QUALITY_THRESHOLD)
                .subAgent(innerPipeline)
                .loopStrategy(LoopMode.condition(messages -> {
                    if (messages == null || messages.isEmpty()) return false;
                    String lastText = messages.get(messages.size() - 1).getText();
                    if (lastText == null || lastText.isBlank()) return false;
                    try {
                        double score = Double.parseDouble(lastText.trim());
                        return score >= QUALITY_THRESHOLD;  // 返回 true = 退出循环
                    } catch (NumberFormatException e) {
                        return false;
                    }
                }))
                .build();
    }

    // 内层顺序 Pipeline 与「三、SequentialAgent」里 SQL 生成 + 评分两步一致；勿用占位 null 塞进 List.of，否则启动建 Bean 时 NPE。
    private SequentialAgent buildSqlQualityPipeline(Model model) {
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
                .instruction("{input}")
                .includeContents(false)
                .outputKey("sql")
                .build();

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
                .instruction("生成的 SQL 是：\n{sql}\n\n原始需求是：\n{input}")
                .includeContents(false)
                .outputKey("score")
                .build();

        return SequentialAgent.builder()
                .name("sql_quality")
                .description("自然语言转 SQL，并评估质量")
                .subAgents(List.of(sqlGenerator, sqlRater))
                .build();
    }
}