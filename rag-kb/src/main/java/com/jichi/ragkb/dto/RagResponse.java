package com.jichi.ragkb.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagResponse {

    private String answer;
    private List<Source> sources;
    private int latencyMs;
    private boolean notFound;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private Long chunkId;
        private Long docId;
        private Integer pageNum;
        private String sectionTitle;
        private String excerpt;        // 相关段落摘要（前200字）
        private double score;          // 相关性分数
        private String docName;     // 文档名称，如"员工手册v2.3.pdf"
    }

    public static RagResponse notFound() {
        return RagResponse.builder()
                .answer("在知识库中未找到与该问题相关的内容。建议您：\n" +
                        "1. 确认问题是否属于该知识库的覆盖范围\n" +
                        "2. 尝试更换关键词提问\n" +
                        "3. 联系相关部门获取准确信息")
                .sources(List.of())
                .notFound(true)
                .build();
    }
}