package com.jichi.prompt.service;

import com.jichi.prompt.entity.AbAssignment;
import com.jichi.prompt.entity.AbExperiment;
import com.jichi.prompt.entity.AbExperimentEntity;
import com.jichi.prompt.entity.AbTestResult;
import com.jichi.prompt.repository.AbExperimentRepository;
import com.jichi.prompt.repository.AbTestResultRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class PromptAbTestService {

    private final PromptVersionService versionService;
    private final AbTestResultRepository resultRepository;
    private final AbExperimentRepository experimentRepository;
    private final Random random = new Random();

    public PromptAbTestService(PromptVersionService versionService,
                                AbTestResultRepository resultRepository,
                                AbExperimentRepository experimentRepository) {
        this.versionService = versionService;
        this.resultRepository = resultRepository;
        this.experimentRepository = experimentRepository;
    }

    /**
     * 根据 A/B 测试配置，为请求分配 Prompt 版本
     *
     * @param experimentId 实验 ID
     * @param userId 用户 ID（同一用户始终分到同一组）
     * @return 分配到的 Prompt 内容
     */
    public AbAssignment assignPrompt(String experimentId, String userId) {
        AbExperiment experiment = loadExperiment(experimentId);

        // 用 userId 哈希保证同一用户固定分到同一组
        int hash = Math.abs(userId.hashCode() % 100);
        String variant = hash < experiment.trafficRatioA() ? "A" : "B";

        String promptContent = variant.equals("A")
                ? versionService.getPromptByVersion(experiment.promptKeyA(), experiment.versionA())
                : versionService.getPromptByVersion(experiment.promptKeyB(), experiment.versionB());

        return new AbAssignment(experimentId, userId, variant, promptContent);
    }

    /**
     * 记录实验结果
     */
    public void recordResult(String experimentId, String userId, String variant,
                              boolean success, Integer userRating) {
        resultRepository.save(new AbTestResult(
                experimentId, userId, variant, success, userRating, LocalDateTime.now()));
    }

    /**
     * 从数据库加载实验配置，只查 RUNNING 状态的实验
     */
    private AbExperiment loadExperiment(String experimentId) {
        AbExperimentEntity entity = experimentRepository
                .findByExperimentIdAndStatus(experimentId, "RUNNING")
                .orElseThrow(() -> new IllegalArgumentException(
                        "实验不存在或已停止：" + experimentId));

        return new AbExperiment(
                entity.getExperimentId(),
                entity.getPromptKeyA(), entity.getVersionA(),
                entity.getPromptKeyB(), entity.getVersionB(),
                entity.getTrafficRatioA()
        );
    }
}