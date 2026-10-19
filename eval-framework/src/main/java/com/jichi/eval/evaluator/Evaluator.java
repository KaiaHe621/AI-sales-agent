package com.jichi.eval.evaluator;

import com.jichi.eval.model.EvalCase;
import com.jichi.eval.model.EvalResult;

public interface Evaluator {

    /**
     * 对单条用例的实际输出进行评估
     *
     * @param evalCase     测试用例（含期望输出）
     * @param actualOutput AI 系统实际返回的内容
     * @return 评估结果
     */
    EvalResult evaluate(EvalCase evalCase, String actualOutput);

    /**
     * 通过阈值，分数高于此值视为通过
     */
    default double passThreshold() {
        return 0.6;
    }
}