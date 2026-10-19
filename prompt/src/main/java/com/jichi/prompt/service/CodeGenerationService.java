package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.jichi.prompt.constant.CodeGenerationPrompts;
import com.jichi.prompt.entity.GeneratedCode;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CodeGenerationService {

    private final DashScopeChatModel chatModel;

    public CodeGenerationService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 根据实体类信息生成完整的 CRUD 接口
     */
    public GeneratedCode generateCrud(String entityDescription, String entityFields) {
        String prompt = String.format("""
                根据以下实体信息，生成完整的 Spring Boot CRUD 接口：
                
                实体名称：%s
                字段信息：%s
                
                需要生成：
                1. Entity 类（包含 JPA 注解和 Lombok）
                2. Repository 接口（继承 JpaRepository）
                3. Service 类（包含增删改查方法）
                4. Controller 类（RESTful 接口，包含 @Valid 参数校验）
                5. DTO 类（请求 DTO + 响应 DTO）
                
                接口规范：
                - POST /api/{entity}           创建
                - GET /api/{entity}/{id}        查询单个
                - GET /api/{entity}?page=0&size=10  分页查询
                - PUT /api/{entity}/{id}        更新
                - DELETE /api/{entity}/{id}     删除
                """, entityDescription, entityFields);

        String code = chatModel.call(new Prompt(
                List.of(new SystemMessage(CodeGenerationPrompts.CODE_GENERATION_SYSTEM),
                        new UserMessage(prompt))
        )).getResult().getOutput().getText();

        return new GeneratedCode(code);
    }
}