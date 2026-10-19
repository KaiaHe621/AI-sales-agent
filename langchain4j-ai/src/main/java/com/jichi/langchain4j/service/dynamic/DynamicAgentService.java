package com.jichi.langchain4j.service.dynamic;

import com.jichi.langchain4j.model.UserRole;
import com.jichi.langchain4j.tools.dynamic.AdminTools;
import com.jichi.langchain4j.tools.dynamic.ModifyTools;
import com.jichi.langchain4j.tools.dynamic.QueryTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DynamicAgentService {

    private final ChatModel chatModel;
    private final QueryTools queryTools;
    private final ModifyTools modifyTools;
    private final AdminTools adminTools;

    public DynamicAgentService(ChatModel chatModel,
                               QueryTools queryTools,
                               ModifyTools modifyTools,
                               AdminTools adminTools) {
        this.chatModel = chatModel;
        this.queryTools = queryTools;
        this.modifyTools = modifyTools;
        this.adminTools = adminTools;
    }

    public String chat(String sessionId, UserRole role, String message) {
        List<Object> tools = buildToolSet(role);

        // 每次请求现场 build，用完即丢，绝不缓存
        // 注意：接口方法有 @MemoryId，必须用 chatMemoryProvider 而不是 chatMemory；
        // 用 chatMemory 时 LangChain4j 会走 provider 路径并用 memoryId 查找，
        // 找不到对应实例就抛 NullPointerException。
        DynamicAssistant agent = AiServices.builder(DynamicAssistant.class)
                .chatModel(chatModel)
                .tools(tools.toArray())
                .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(10))
                .build();

        return agent.chat(sessionId, message);
    }

    private List<Object> buildToolSet(UserRole role) {
        List<Object> tools = new ArrayList<>();
        tools.add(unwrap(queryTools));                                   // 所有角色都有查询权限

        if (role == UserRole.MEMBER || role == UserRole.ADMIN) {
            tools.add(unwrap(modifyTools));                              // 会员和管理员有修改权限
        }
        if (role == UserRole.ADMIN) {
            tools.add(unwrap(adminTools));                               // 管理员独享管理工具
        }
        return tools;
    }

    /**
     * 剥离 Spring AOP CGLIB 代理，拿到真实对象。
     * 若项目里有 @Aspect 拦截了 @Tool 方法（如统一异常处理），工具类会被代理，
     * LangChain4j 扫描不到 @Tool 注解，必须先解包再传入。
     */
    private Object unwrap(Object bean) {
        try {
            return AopUtils.isAopProxy(bean)
                    ? ((Advised) bean).getTargetSource().getTarget()
                    : bean;
        } catch (Exception e) {
            return bean;
        }
    }
}