package com.jichi.eval.evaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.eval.model.EvalCase;
import com.jichi.eval.model.EvalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component("judgeEvaluator")
public class JudgeEvaluator implements Evaluator {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public JudgeEvaluator(ChatClient.Builder chatClientBuilder, ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    private static final String JUDGE_PROMPT_TEMPLATE = """
            你是一名 AI 系统评估专家。请根据以下信息对 AI 系统的回答进行评估。
            
            ## 评估任务
            - 用户问题：%s
            - 参考答案（Golden Answer）：%s
            - AI 系统实际回答：%s
            
            ## 评分维度
            请从以下两个维度分别打分（1-5 分整数）：
            
            **事实正确性（factual_score）**
            - 5分：完全正确，与参考答案语义一致，无任何幻觉
            - 4分：基本正确，存在细微措辞差异但语义等价
            - 3分：部分正确，有少量事实错误或遗漏
            - 2分：明显错误，核心事实有误
            - 1分：完全错误或无关
            
            **完整性（completeness_score）**
            - 5分：完整覆盖参考答案中的所有要点
            - 4分：覆盖了大部分要点（>80%%）
            - 3分：覆盖了一半左右的要点
            - 2分：只覆盖了少量要点
            - 1分：几乎未覆盖任何要点
            
            ## 输出格式
            请先写出你的分析推理（2-3句），再输出 JSON：
            
            ```json
            {
              "analysis": "你的推理过程",
              "factual_score": <1-5的整数>,
              "completeness_score": <1-5的整数>
            }
            ```
            只输出 JSON，不要有任何其他内容。
            """;

    @Override
    public EvalResult evaluate(EvalCase evalCase, String actualOutput) {
        if (actualOutput == null || actualOutput.isBlank()) {
            return buildResult(evalCase, actualOutput, 0.0, "输出为空");
        }

        String judgePrompt = String.format(JUDGE_PROMPT_TEMPLATE,
                evalCase.getInput(),
                evalCase.getExpectedOutput(),
                actualOutput);

        try {
            String judgeResponse = chatClient.prompt()
                    .user(judgePrompt)
                    .call()
                    .content();

            return parseJudgeResponse(evalCase, actualOutput, judgeResponse);

        } catch (Exception e) {
            log.error("[{}] Judge 调用失败：{}", evalCase.getId(), e.getMessage());
            return buildResult(evalCase, actualOutput, 0.0, "Judge 调用失败：" + e.getMessage());
        }
    }

    private EvalResult parseJudgeResponse(EvalCase evalCase, String actualOutput,
                                          String judgeResponse) {
        try {
            // 提取 JSON 块（Judge 有时会在 JSON 前后带文字）
            String jsonStr = extractJson(judgeResponse);
            JsonNode node = objectMapper.readTree(jsonStr);

            int factualScore = node.path("factual_score").asInt(1);
            int completenessScore = node.path("completeness_score").asInt(1);
            String analysis = node.path("analysis").asText("");

            // 归一化到 0-1：两个维度取平均，原始范围 1-5
            double normalizedScore = ((factualScore + completenessScore) / 2.0 - 1) / 4.0;
            String reason = String.format(
                    "事实正确性=%d/5，完整性=%d/5。分析：%s",
                    factualScore, completenessScore, analysis);

            return buildResult(evalCase, actualOutput, normalizedScore, reason);

        } catch (Exception e) {
            log.warn("[{}] 解析 Judge 响应失败，原始响应：{}", evalCase.getId(), judgeResponse);
            return buildResult(evalCase, actualOutput, 0.0, "Judge 响应解析失败：" + e.getMessage());
        }
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start == -1 || end == -1 || start > end) {
            return text;
        }
        return text.substring(start, end + 1);
    }

    private EvalResult buildResult(EvalCase evalCase, String actualOutput,
                                   double score, String reason) {
        return EvalResult.builder()
                .caseId(evalCase.getId())
                .actualOutput(actualOutput)
                .score(score)
                .dimension("llm-judge")
                .reason(reason)
                .passed(score >= passThreshold())
                .build();
    }

    @Override
    public double passThreshold() {
        return 0.5;  // 对应原始分 3/5
    }
}