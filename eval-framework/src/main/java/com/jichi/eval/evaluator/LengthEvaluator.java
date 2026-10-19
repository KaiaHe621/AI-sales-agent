package com.jichi.eval.evaluator;

import com.jichi.eval.model.EvalCase;
import com.jichi.eval.model.EvalResult;
import org.springframework.stereotype.Component;

@Component("lengthEvaluator")
public class LengthEvaluator implements Evaluator {

    private static final int MIN_LENGTH = 10;
    private static final int MAX_LENGTH = 1000;

    @Override
    public EvalResult evaluate(EvalCase evalCase, String actualOutput) {
        if (actualOutput == null || actualOutput.isBlank()) {
            return buildResult(evalCase, "", 0.0, "输出为空");
        }

        int length = actualOutput.trim().length();

        if (length < MIN_LENGTH) {
            double score = (double) length / MIN_LENGTH;
            return buildResult(evalCase, actualOutput, score,
                    String.format("输出过短：%d 字符（最小要求 %d）", length, MIN_LENGTH));
        }

        if (length > MAX_LENGTH) {
            double score = Math.max(0.0, 1.0 - (double)(length - MAX_LENGTH) / MAX_LENGTH);
            return buildResult(evalCase, actualOutput, score,
                    String.format("输出过长：%d 字符（最大建议 %d）", length, MAX_LENGTH));
        }

        return buildResult(evalCase, actualOutput, 1.0,
                String.format("输出长度正常：%d 字符", length));
    }

    private EvalResult buildResult(EvalCase evalCase, String actualOutput,
                                   double score, String reason) {
        return EvalResult.builder()
                .caseId(evalCase.getId())
                .actualOutput(actualOutput)
                .score(score)
                .dimension("length")
                .reason(reason)
                .passed(score >= passThreshold())
                .build();
    }
}