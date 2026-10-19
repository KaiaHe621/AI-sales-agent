package com.jichi.agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlanAndExecuteAgent {

    private static final Logger log = LoggerFactory.getLogger(PlanAndExecuteAgent.class);

    private final TaskPlanner planner;
    private final StepExecutor executor;

    private static final List<String> AVAILABLE_TOOLS = List.of(
            "getWeather（查天气）",
            "getCurrentDateTime（查时间）",
            "getExchangeRate（查汇率）",
            "getOrderTracking（查订单状态）",
            "searchProducts（搜索商品）"
    );

    public PlanAndExecuteAgent(TaskPlanner planner, StepExecutor executor) {
        this.planner = planner;
        this.executor = executor;
    }

    public ExecutionResult run(String task) {
        log.info("开始执行任务：{}", task);

        List<String> steps = planner.plan(task, AVAILABLE_TOOLS);
        log.info("生成计划，共 {} 步：{}", steps.size(), steps);

        Map<Integer, String> results = new LinkedHashMap<>();

        for (int i = 0; i < steps.size(); i++) {
            String step = steps.get(i);
            int stepNo = i + 1;
            log.info("执行 Step {}：{}", stepNo, step);

            try {
                String result = executor.execute(step, results);
                results.put(stepNo, result);
                log.info("Step {} 完成", stepNo);
            } catch (Exception e) {
                log.error("Step {} 执行失败：{}", stepNo, e.getMessage());
                results.put(stepNo, "执行失败：" + e.getMessage());
                // 这里可以 break（失败就停）或 continue（跳过继续执行后续步骤）
                // 鸡哥建议：非关键步骤 continue，关键步骤 break
            }
        }

        return new ExecutionResult(task, steps, results);
    }

    public record ExecutionResult(
            String originalTask,
            List<String> plan,
            Map<Integer, String> stepResults
    ) {
        public String summary() {
            StringBuilder sb = new StringBuilder();
            sb.append("任务：").append(originalTask).append("\n\n");
            sb.append("执行步骤：\n");
            for (int i = 0; i < plan.size(); i++) {
                int stepNo = i + 1;
                sb.append("Step ").append(stepNo).append("：").append(plan.get(i)).append("\n");
                sb.append("结果：").append(stepResults.getOrDefault(stepNo, "未执行")).append("\n\n");
            }
            return sb.toString();
        }
    }
}