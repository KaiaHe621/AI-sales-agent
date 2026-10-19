package com.jichi.eval.controller;

import com.jichi.eval.model.EvalReport;
import com.jichi.eval.runner.EvalRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eval")
@RequiredArgsConstructor
public class EvalController {

    private final EvalRunner evalRunner;

    /**
     * 触发一次完整评估
     *
     * @param endpoint  被测 AI 系统接口地址（URL 编码后传入）
     * @param evaluator 使用的评估器名称，默认 keywordEvaluator
     */
    @PostMapping("/run")
    public EvalReport run(
            @RequestParam String endpoint,
            @RequestParam(defaultValue = "keywordEvaluator") String evaluator
    ) throws Exception {
        return evalRunner.run(endpoint, evaluator);
    }
}