package com.jichi.ragkb.controller;

import com.jichi.ragkb.dto.ApiResponse;
import com.jichi.ragkb.dto.EvalReport;
import com.jichi.ragkb.entity.DocChunk;
import com.jichi.ragkb.entity.EvalDataset;
import com.jichi.ragkb.repository.DocChunkRepository;
import com.jichi.ragkb.repository.EvalDatasetRepository;
import com.jichi.ragkb.security.UserContext;
import com.jichi.ragkb.service.EvalService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/eval")
@RequiredArgsConstructor
public class EvalController {

    private final EvalService evalService;
    private final EvalDatasetRepository datasetRepository;
    private final DocChunkRepository chunkRepository;

    /**
     * 触发评估（管理员专用）
     */
    @PostMapping("/{kbId}/run")
    public ApiResponse<EvalReport> runEval(
            @PathVariable Long kbId,
            @RequestParam(defaultValue = "latest") String version) {
        EvalReport report = evalService.runEvaluation(kbId, version);
        return ApiResponse.ok(report);
    }

    /**
     * 查看历史评估对比
     */
    @GetMapping("/{kbId}/history")
    public ApiResponse<List<EvalReport>> getHistory(@PathVariable Long kbId) {
        return ApiResponse.ok(evalService.compareVersions(kbId));
    }

    // ==================== 评估数据集管理 ====================

    /**
     * 查询知识库的评估数据集
     */
    @GetMapping("/{kbId}/dataset")
    public ApiResponse<List<EvalDataset>> listDataset(@PathVariable Long kbId) {
        return ApiResponse.ok(datasetRepository.findByKbId(kbId));
    }

    /**
     * 新增评估问题
     */
    @PostMapping("/{kbId}/dataset")
    public ApiResponse<EvalDataset> addQuestion(
            @PathVariable Long kbId,
            @RequestBody EvalDatasetRequest req) {
        EvalDataset item = new EvalDataset();
        item.setKbId(kbId);
        item.setQuestion(req.getQuestion());
        item.setExpectedAnswer(req.getExpectedAnswer());
        item.setExpectedChunkIds(req.getExpectedChunkIds());
        item.setCreatedBy(UserContext.getUserId());
        return ApiResponse.ok(datasetRepository.save(item));
    }

    /**
     * 更新评估问题（含回填 expectedChunkIds）
     */
    @PutMapping("/{kbId}/dataset/{id}")
    public ApiResponse<EvalDataset> updateQuestion(
            @PathVariable Long kbId,
            @PathVariable Long id,
            @RequestBody EvalDatasetRequest req) {
        EvalDataset item = datasetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评估数据不存在"));
        if (req.getQuestion() != null) item.setQuestion(req.getQuestion());
        if (req.getExpectedAnswer() != null) item.setExpectedAnswer(req.getExpectedAnswer());
        if (req.getExpectedChunkIds() != null) item.setExpectedChunkIds(req.getExpectedChunkIds());
        return ApiResponse.ok(datasetRepository.save(item));
    }

    /**
     * 删除评估问题
     */
    @DeleteMapping("/{kbId}/dataset/{id}")
    public ApiResponse<Void> deleteQuestion(
            @PathVariable Long kbId,
            @PathVariable Long id) {
        datasetRepository.deleteById(id);
        return ApiResponse.ok(null);
    }

    // ==================== Chunk 查询（用于标注 expectedChunkIds） ====================

    /**
     * 查询知识库下的所有 Chunk（只返回 id、docId、chunkIndex 和内容摘要）
     */
    @GetMapping("/{kbId}/chunks")
    public ApiResponse<List<ChunkSummary>> listChunks(@PathVariable Long kbId) {
        List<DocChunk> chunks = chunkRepository.findByKbId(kbId);
        List<ChunkSummary> summaries = chunks.stream().map(c -> {
            ChunkSummary s = new ChunkSummary();
            s.setId(c.getId());
            s.setDocId(c.getDocId());
            s.setChunkIndex(c.getChunkIndex());
            s.setContent(c.getContent().length() > 200
                    ? c.getContent().substring(0, 200) + "..."
                    : c.getContent());
            s.setTokenCount(c.getTokenCount());
            return s;
        }).toList();
        return ApiResponse.ok(summaries);
    }

    @Data
    public static class EvalDatasetRequest {
        private String question;
        private String expectedAnswer;
        private Long[] expectedChunkIds;
    }

    @Data
    public static class ChunkSummary {
        private Long id;
        private Long docId;
        private Integer chunkIndex;
        private String content;
        private Integer tokenCount;
    }
}