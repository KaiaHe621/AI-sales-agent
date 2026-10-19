package com.jichi.agentscope.config;

import com.alibaba.cloud.ai.agent.agentscope.AgentScopeAgent;
import com.alibaba.cloud.ai.graph.agent.flow.agent.ParallelAgent;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ResearchPipelineConfig {

    @Bean("parallelResearchAgent")
    public ParallelAgent parallelResearchAgent(Model model) {

        // 技术分析 Agent
        AgentScopeAgent techResearcher = buildResearcher(model,
                "tech_researcher",
                "你是一名技术分析师。从技术视角分析给定主题：关键技术、发展趋势、技术挑战。写 2-3 段。",
                "tech_analysis");

        // 商业/财务分析 Agent
        AgentScopeAgent financeResearcher = buildResearcher(model,
                "finance_researcher",
                "你是一名金融分析师。从商业视角分析给定主题：市场规模、投资趋势、商业模式。写 2-3 段。",
                "finance_analysis");

        // 市场分析 Agent
        AgentScopeAgent marketResearcher = buildResearcher(model,
                "market_researcher",
                "你是一名市场分析师。从行业视角分析给定主题：竞争格局、增长驱动力、行业挑战。写 2-3 段。",
                "market_analysis");

        return ParallelAgent.builder()
                .name("parallel_research_agent")
                .description("从技术、商业、市场三角度并行研究主题")
                .subAgents(List.of(techResearcher, financeResearcher, marketResearcher))
                .mergeStrategy(new ParallelAgent.DefaultMergeStrategy())  // 默认合并策略
                .mergeOutputKey("research_report")  // 合并结果存入 "research_report" 键
                .maxConcurrency(3)                  // 最多 3 个 Agent 同时运行
                .build();
    }

    private AgentScopeAgent buildResearcher(Model model, String name,
                                            String sysPrompt, String outputKey) {
        ReActAgent.Builder builder = ReActAgent.builder()
                .name(name)
                .model(model)
                .sysPrompt(sysPrompt)
                .memory(new InMemoryMemory());

        return AgentScopeAgent.fromBuilder(builder)
                .name(name)
                .description("从特定角度研究分析")
                .instruction("请分析以下主题：{input}")
                .includeContents(false)
                .outputKey(outputKey)
                .build();
    }
}