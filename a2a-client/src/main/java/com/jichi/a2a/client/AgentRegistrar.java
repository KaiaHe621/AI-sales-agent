package com.jichi.a2a.client;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AgentRegistrar implements ApplicationRunner {

    private final AgentRegistry registry;

    public AgentRegistrar(AgentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 注册本地 Sales Agent（开发时）
        registry.register("http://localhost:8080");

        // 生产环境从配置文件读取 Agent URL 列表
        // agentUrls.forEach(registry::register);
    }
}