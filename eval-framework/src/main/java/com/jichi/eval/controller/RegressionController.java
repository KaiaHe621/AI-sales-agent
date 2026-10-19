package com.jichi.eval.controller;

import com.jichi.eval.model.EvalReport;
import com.jichi.eval.regression.BaselineStore;
import com.jichi.eval.regression.RegressionRunner;
import com.jichi.eval.runner.EvalRunner;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eval")
@RequiredArgsConstructor
public class RegressionController {

    private final EvalRunner evalRunner;
    private final BaselineStore baselineStore;
    private final RegressionRunner regressionRunner;

    /** 跑评估并保存为 Baseline */
    @PostMapping("/baseline/save")
    public ResponseEntity<String> saveBaseline(
            @RequestBody RegressionRequest request
    ) throws Exception {
        String evaluator = request.getEvaluator() != null
                ? request.getEvaluator() : "keywordEvaluator";
        EvalReport report = evalRunner.run(request.getEndpoint(), evaluator);
        baselineStore.save(request.getBaselineName(), report);
        return ResponseEntity.ok("Baseline 已保存：" + request.getBaselineName()
                + "（均分：" + String.format("%.3f", report.getAvgScore()) + "）");
    }

    /** 跑评估并与指定 Baseline 对比，检测 Regression */
    @PostMapping("/regression/check")
    public ResponseEntity<?> checkRegression(
            @RequestBody RegressionRequest request
    ) throws Exception {
        String evaluator = request.getEvaluator() != null
                ? request.getEvaluator() : "keywordEvaluator";
        EvalReport currentReport = evalRunner.run(request.getEndpoint(), evaluator);
        EvalReport baselineReport = baselineStore.load(request.getBaselineName());

        RegressionRunner.RegressionReport regressionReport =
                regressionRunner.compare(baselineReport, currentReport);

        if (regressionReport.isHasRegression()) {
            return ResponseEntity.status(422).body(regressionReport);
        }
        return ResponseEntity.ok(regressionReport);
    }

    @lombok.Data
    public static class RegressionRequest {
        private String endpoint;
        private String evaluator;
        private String baselineName;
    }
}