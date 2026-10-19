package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class IncrementalCodeGenerationService {

    private final DashScopeChatModel chatModel;

    public IncrementalCodeGenerationService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 渐进式生成：Entity → Repository → Service
     * 每一步把上一步的代码作为上下文，保证字段/方法一致
     *
     * @return 文件名 → 代码内容 的 Map，如 {"UserEntity.java": "...", "UserRepository.java": "..."}
     */
    public Map<String, String> generateProjectIncrementally(String projectDescription) {
        // 用局部变量，每次请求独立，不存在并发串数据问题
        Map<String, String> generatedFiles = new LinkedHashMap<>();

        // Step 1: 生成 Entity
        String entityCode = chatModel.call(new Prompt(
                new UserMessage("根据以下需求生成 Entity 类（使用 Lombok + JPA 注解）：\n" + projectDescription)
        )).getResult().getOutput().getText();
        generatedFiles.put("Entity.java", entityCode);

        // Step 2: 基于 Entity 生成 Repository（把 Entity 代码原文贴进去）
        String repoCode = chatModel.call(new Prompt(
                new UserMessage(String.format("""
                        基于以下 Entity 类，生成对应的 JPA Repository 接口：
                        
                        ```java
                        %s
                        ```
                        
                        需要包含常用的自定义查询方法（按状态查询、分页、按时间范围查询）。
                        """, entityCode))
        )).getResult().getOutput().getText();
        generatedFiles.put("Repository.java", repoCode);

        // Step 3: 基于 Entity + Repository 生成 Service（两份代码都贴进去）
        String serviceCode = chatModel.call(new Prompt(
                new UserMessage(String.format("""
                        基于以下 Entity 和 Repository，生成 Service 类：
                        
                        Entity:
                        ```java
                        %s
                        ```
                        
                        Repository:
                        ```java
                        %s
                        ```
                        
                        Service 需要包含完整的增删改查业务逻辑和异常处理，方法签名与 Repository 保持一致。
                        """, entityCode, repoCode))
        )).getResult().getOutput().getText();
        generatedFiles.put("Service.java", serviceCode);

        return generatedFiles;
    }
}