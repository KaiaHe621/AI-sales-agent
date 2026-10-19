package com.jichi.prompt.service;

import com.jichi.prompt.entity.PromptTemplateEntity;
import com.jichi.prompt.entity.PromptVersionInfo;
import com.jichi.prompt.repository.PromptTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Transactional
public class PromptVersionService {

    private final PromptTemplateRepository repository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public PromptVersionService(PromptTemplateRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取当前激活的 Prompt 内容（带缓存）
     */
    public String getActivePrompt(String key) {
        String env = System.getProperty("spring.profiles.active", "production");
        return cache.computeIfAbsent(key + ":" + env, k ->
                repository.findByTemplateKeyAndStatusAndEnvironment(key, "ACTIVE", env)
                        .map(PromptTemplateEntity::getContent)
                        .orElseThrow(() -> new IllegalStateException("没有激活的 Prompt：" + key)));
    }

    /**
     * 发布新版本（当前激活的自动 ARCHIVED，新版本变为 ACTIVE）
     */
    public void publishVersion(String key, String version, String content,
                               String description, String environment) {
        // 把当前 ACTIVE 的归档
        repository.findByTemplateKeyAndStatusAndEnvironment(key, "ACTIVE", environment)
                .ifPresent(current -> {
                    current.setStatus("ARCHIVED");
                    repository.save(current);
                });

        // 新版本直接 ACTIVE
        PromptTemplateEntity newVersion = new PromptTemplateEntity();
        newVersion.setTemplateKey(key);
        newVersion.setVersion(version);
        newVersion.setContent(content);
        newVersion.setDescription(description);
        newVersion.setStatus("ACTIVE");
        newVersion.setEnvironment(environment);
        repository.save(newVersion);

        // 清缓存
        cache.remove(key + ":" + environment);
    }

    /**
     * 回滚到指定版本
     */
    public void rollbackTo(String key, String targetVersion, String environment) {
        PromptTemplateEntity target = repository
                .findByTemplateKeyAndVersionAndEnvironment(key, targetVersion, environment)
                .orElseThrow(() -> new IllegalArgumentException("版本不存在：" + targetVersion));

        // 归档当前激活版本
        repository.findByTemplateKeyAndStatusAndEnvironment(key, "ACTIVE", environment)
                .ifPresent(current -> {
                    current.setStatus("ARCHIVED");
                    repository.save(current);
                });

        // 将目标版本重新激活
        target.setStatus("ACTIVE");
        repository.save(target);

        // 清缓存
        cache.remove(key + ":" + environment);
    }

    /**
     * 查询版本历史
     */
    public List<PromptVersionInfo> getVersionHistory(String key, String environment) {
        return repository.findByTemplateKeyAndEnvironmentOrderByCreatedAtDesc(key, environment)
                .stream()
                .map(e -> new PromptVersionInfo(
                        e.getVersion(),
                        e.getStatus(),
                        e.getDescription(),
                        e.getCreatedBy(),
                        e.getCreatedAt()))
                .toList();
    }

    /**
     * 按指定版本号获取 Prompt 内容（A/B 测试场景专用：需要同时跑两个固定版本）
     */
    public String getPromptByVersion(String key, String version) {
        String env = System.getProperty("spring.profiles.active", "production");
        return repository.findByTemplateKeyAndVersionAndEnvironment(key, version, env)
                .map(PromptTemplateEntity::getContent)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Prompt 不存在：key=" + key + " version=" + version));
    }
}
