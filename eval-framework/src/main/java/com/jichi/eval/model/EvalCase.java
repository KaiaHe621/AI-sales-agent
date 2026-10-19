package com.jichi.eval.model;

import lombok.Data;
import java.util.List;

@Data
public class EvalCase {

    /** 用例 ID */
    private String id;

    /** 用户输入 */
    private String input;

    /** 期望输出（Golden Answer），供 LLM-as-Judge 类评估器参考 */
    private String expectedOutput;

    /**
     * 明确的核心关键词列表，供 KeywordEvaluator 使用。
     * 设计原则：每个词应足够短（2~8 个字），能在语义等价的不同表述中都命中。
     * 优先级高于从 expectedOutput 自动拆分。
     */
    private List<String> keywords;

    /** 用例标签，用于分组统计（如 "factual" / "format" / "safety"） */
    private String tag;
}