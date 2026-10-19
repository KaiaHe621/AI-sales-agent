package com.jichi.prompt.repository;

import com.jichi.prompt.entity.AbExperimentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AbExperimentRepository extends JpaRepository<AbExperimentEntity, Long> {

    Optional<AbExperimentEntity> findByExperimentIdAndStatus(String experimentId, String status);
}