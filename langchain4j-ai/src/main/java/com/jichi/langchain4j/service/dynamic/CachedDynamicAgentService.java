package com.jichi.langchain4j.service.dynamic;

import com.jichi.langchain4j.model.UserRole;
import com.jichi.langchain4j.tools.dynamic.AdminTools;
import com.jichi.langchain4j.tools.dynamic.ModifyTools;
import com.jichi.langchain4j.tools.dynamic.QueryTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class CachedDynamicAgentService {

    private final ChatModel chatModel;
    private final QueryTools queryTools;
    private final ModifyTools modifyTools;
    private final AdminTools adminTools;

    // key = 角色，value = 对应的 Agent 实例（启动时预热，请求时直接复用）
    private final Map<UserRole, DynamicAssistant> agentCache = new EnumMap<>(UserRole.class);

    public CachedDynamicAgentService(ChatModel chatModel,
                                     QueryTools queryTools,
                                     ModifyTools modifyTools,
                                     AdminTools adminTools) {
        this.chatModel = chatModel;
        this.queryTools = queryTools;
        this.modifyTools = modifyTools;
        this.adminTools = adminTools;
    }

    @PostConstruct
    public void init() throws Exception {
        // 应用启动时一次性按角色预创建，后续请求直接从缓存取
        agentCache.put(UserRole.GUEST,  buildAgent(UserRole.GUEST));
        agentCache.put(UserRole.MEMBER, buildAgent(UserRole.MEMBER));
        agentCache.put(UserRole.ADMIN,  buildAgent(UserRole.ADMIN));
    }

    public String chat(String sessionId, UserRole role, String message) {
        return agentCache.get(role).chat(sessionId, message);
    }

    private DynamicAssistant buildAgent(UserRole role) throws Exception {
        List<Object> tools = new ArrayList<>();
        tools.add(unwrap(queryTools));
        if (role == UserRole.MEMBER || role == UserRole.ADMIN) tools.add(unwrap(modifyTools));
        if (role == UserRole.ADMIN) tools.add(unwrap(adminTools));

        return AiServices.builder(DynamicAssistant.class)
                .chatModel(chatModel)
                .tools(tools.toArray())
                // chatMemoryProvider 按 sessionId 为每个用户单独维护记忆，实例复用但记忆不混
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    private Object unwrap(Object bean) throws Exception {
        return AopUtils.isAopProxy(bean)
                ? ((Advised) bean).getTargetSource().getTarget()
                : bean;
    }
}