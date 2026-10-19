package com.jichi.prompt.repository;

import com.jichi.prompt.entity.AbTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AbTestResultRepository extends JpaRepository<AbTestResult, Long> {

    // 查某个实验的所有结果
    List<AbTestResult> findByExperimentId(String experimentId);

    // 查某个实验某个分组的所有结果
    List<AbTestResult> findByExperimentIdAndVariant(String experimentId, String variant);

    // 统计某个分组的总请求数
    long countByExperimentIdAndVariant(String experimentId, String variant);

    // 查有用户评分的结果（用于计算平均分）
    @Query("SELECT r FROM AbTestResult r WHERE r.experimentId = :experimentId " +
           "AND r.variant = :variant AND r.userRating IS NOT NULL")
    List<AbTestResult> findRatedResults(String experimentId, String variant);
}