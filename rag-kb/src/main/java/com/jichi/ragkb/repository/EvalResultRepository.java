package com.jichi.ragkb.repository;

import com.jichi.ragkb.dto.EvalReport;
import com.jichi.ragkb.entity.EvalResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EvalResultRepository extends JpaRepository<EvalResult, Long> {

    @Query("""
            SELECT new com.jichi.ragkb.dto.EvalReport(
                d.kbId, r.evalVersion, COUNT(r), 
                SUM(CASE WHEN r.hit = true THEN 1 ELSE 0 END),
                AVG(CASE WHEN r.hit = true THEN 1.0 ELSE 0.0 END),
                AVG(CASE WHEN r.rank > 0 THEN 1.0 / r.rank ELSE 0.0 END),
                AVG(COALESCE(r.faithfulness, 0)),
                MAX(r.evalAt))
            FROM EvalResult r JOIN EvalDataset d ON r.datasetId = d.id
            WHERE d.kbId = :kbId
            GROUP BY d.kbId, r.evalVersion
            ORDER BY MAX(r.evalAt) DESC
            """)
    List<EvalReport> aggregateByVersion(Long kbId);
}