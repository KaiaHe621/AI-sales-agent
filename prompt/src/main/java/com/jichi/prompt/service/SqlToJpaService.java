package com.jichi.prompt.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

@Service
public class SqlToJpaService {

    private final DashScopeChatModel chatModel;

    public SqlToJpaService(DashScopeChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String convertSqlToJpa(String sql, String entityName) {
        return chatModel.call(new Prompt(new UserMessage(String.format("""
                将以下 SQL 查询转换为 Spring Data JPA 的 Repository 方法：
                
                SQL：
                %s
                
                实体类名：%s
                
                要求：
                1. 优先使用方法命名规范（findByXxxAndYyy）
                2. 复杂查询用 @Query（JPQL，不用 native SQL）
                3. 如果涉及分页，参数加 Pageable
                4. 输出完整的 Repository 方法定义（含注解）
                """, sql, entityName))
        )).getResult().getOutput().getText();
    }
}