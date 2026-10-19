package com.jichi.springaialibaba.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

// prefix = "app.ai" 对应 application.yml 里的 app.ai 层级
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProviderProperties {

    // 对应 app.ai.default-provider，没配则默认 deepseek
    private String defaultProvider = "deepseek";

    // 对应 app.ai.providers，key 是厂商名（deepseek/qwen），value 是该厂商的配置
    private Map<String, ProviderConfig> providers = new HashMap<>();

    public record ProviderConfig(String model) {
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public Map<String, ProviderConfig> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderConfig> providers) {
        this.providers = providers;
    }
}