package com.jichi.langchain4j.repository;

import com.jichi.langchain4j.model.TenantPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantPromptRepository extends JpaRepository<TenantPrompt, Long> {
    Optional<TenantPrompt> findByTenantId(String tenantId);
}