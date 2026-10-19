package com.jichi.ragkb.service;

import com.jichi.ragkb.dto.EvalReport;
import com.jichi.ragkb.dto.RagResponse;
import com.jichi.ragkb.entity.*;
import com.jichi.ragkb.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvalService {

    private final EvalDatasetRepository datasetRepository;
    private final EvalResultRepository resultRepository;
    private final EnhancedRetrieverService retriever;
    private final RerankerService rerankerService;
    private final ConfidenceFilter confidenceFilter;
    private final HallucinationChecker hallucinationChecker;
    private final StreamingRagService ragService;

    /**
     * 运行完整评估流水线。
     *
     * @param kbId        知识库 ID
     * @param evalVersion 评估版本标识（如 v1_chunk512_hybrid_reranker）
     * @return 评估摘要报告
     */
    public EvalReport runEvaluation(Long kbId, String evalVersion) {
        List<EvalDataset> questions = datasetRepository.findByKbId(kbId);
        if (questions.isEmpty()) {
            throw new RuntimeException("知识库 " + kbId + " 没有评估数据集，请先录入标准问题");
        }

        log.info("[Eval] 开始评估：kbId={}，version={}，问题数={}", kbId, evalVersion, questions.size());

        List<EvalResult> results = new ArrayList<>();
        int hits = 0;
        double mrr = 0.0;
        double totalFaithfulness = 0.0;
        int evalCount = 0;

        for (EvalDataset question : questions) {
            try {
                EvalResult result = evaluateOne(question, kbId, evalVersion);
                results.add(result);

                if (result.getHit()) hits++;
                if (result.getRank() != null && result.getRank() > 0) {
                    mrr += 1.0 / result.getRank();
                }
                if (result.getFaithfulness() != null) {
                    totalFaithfulness += result.getFaithfulness();
                    evalCount++;
                }

            } catch (Exception e) {
                log.error("[Eval] 问题评估失败：questionId={}，error={}", question.getId(), e.getMessage());
            }
        }

        // 批量保存评估结果
        resultRepository.saveAll(results);

        double hitRate = questions.isEmpty() ? 0 : (double) hits / questions.size();
        double mrrScore = questions.isEmpty() ? 0 : mrr / questions.size();
        double avgFaithfulness = evalCount == 0 ? 0 : totalFaithfulness / evalCount;

        EvalReport report = EvalReport.builder()
                .kbId(kbId)
                .evalVersion(evalVersion)
                .totalQuestions(questions.size())
                .hitCount(hits)
                .hitRate(hitRate)
                .mrr(mrrScore)
                .avgFaithfulness(avgFaithfulness)
                .evalAt(LocalDateTime.now())
                .build();

        log.info("[Eval] 评估完成：hitRate={}%，MRR={}，faithfulness={}",
                String.format("%.2f", hitRate * 100),
                String.format("%.4f", mrrScore),
                String.format("%.4f", avgFaithfulness));

        return report;
    }

    private EvalResult evaluateOne(EvalDataset question, Long kbId, String evalVersion) {
        // 执行检索
        List<HybridRetrieverService.ScoredChunk> candidates =
                retriever.retrieveWithHyde(question.getQuestion(), List.of(kbId), 20);
        List<HybridRetrieverService.ScoredChunk> reranked =
                rerankerService.rerank(question.getQuestion(), candidates, 10);

        // 计算 Hit Rate 和 MRR
        Long[] expectedChunkIds = question.getExpectedChunkIds();
        boolean hit = false;
        int rank = 0;

        if (expectedChunkIds != null && expectedChunkIds.length > 0) {
            Set<Long> expected = Set.of(expectedChunkIds);
            for (int i = 0; i < reranked.size(); i++) {
                if (expected.contains(reranked.get(i).id())) {
                    hit = true;
                    rank = i + 1;  // 1-based
                    break;
                }
            }
        }

        // 生成回答并评估忠实性（抽样评估，降低成本）
        String actualAnswer = null;
        Double faithfulness = null;

        if (question.getExpectedAnswer() != null) {
            RagResponse response = ragService.syncQuery(
                    question.getQuestion(), List.of(kbId), "eval-session");
            actualAnswer = response.getAnswer();

            // 忠实性检测（每次评估都跑，但仅限标准问题集）
            String context = candidates.stream()
                    .limit(5)
                    .map(HybridRetrieverService.ScoredChunk::content)
                    .collect(Collectors.joining("\n\n"));

            HallucinationChecker.FaithfulnessResult faithResult =
                    hallucinationChecker.check(question.getQuestion(), actualAnswer, context);
            faithfulness = faithResult.score();
        }

        EvalResult result = new EvalResult();
        result.setDatasetId(question.getId());
        result.setEvalVersion(evalVersion);
        result.setHit(hit);
        result.setRank(rank > 0 ? rank : null);
        result.setActualAnswer(actualAnswer);
        result.setFaithfulness(faithfulness);
        result.setEvalAt(LocalDateTime.now());

        return result;
    }

    /**
     * 对比不同评估版本的指标，生成对比报告。
     */
    public List<EvalReport> compareVersions(Long kbId) {
        // 从数据库查历史评估结果，按版本聚合
        return resultRepository.aggregateByVersion(kbId);
    }
}