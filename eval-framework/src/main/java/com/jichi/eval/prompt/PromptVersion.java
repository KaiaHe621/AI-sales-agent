package com.jichi.eval.prompt;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class PromptVersion {

    /** 版本标识符，如 "v1.0" / "v1.1-concise" */
    private String version;

    /** 变更说明 */
    private String description;

    /** System Prompt 内容 */
    private String systemPrompt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}