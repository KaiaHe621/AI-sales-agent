package com.jichi.springaialibaba.service;

import com.jichi.springaialibaba.config.AiProviderProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class ModelRouterService {

    // key：厂商名（deepseek/qwen），value：对应的 ChatClient
    // 用 Map 存储是为了支持按名称动态查找，避免大量 if-else
    private final Map<String, ChatClient> chatClients;
    private final String defaultProvider;
    private final AiProviderProperties properties;

    public ModelRouterService(
            // @Qualifier 指定注入 MultiModelConfig 里定义的 Bean，避免和 @Primary 冲突
            @Qualifier("primaryChatClient") ChatClient primaryChatClient,
            @Qualifier("backupChatClient") ChatClient backupChatClient,
            AiProviderProperties properties) {

        this.properties = properties;
        this.defaultProvider = properties.getDefaultProvider();
        // Map.of 创建不可变 Map，运行期不会被意外修改
        this.chatClients = Map.of(
                "deepseek", primaryChatClient,
                "qwen", backupChatClient
        );
    }

    /**
     * 按业务场景选择模型：不同场景有不同的最优解
     * - code-review：DeepSeek 代码能力强
     * - chinese-text / customer-service：通义千问中文效果好、国内合规
     */
    public ChatClient selectForScene(String scene) {
        return switch (scene) {
            case "code-review" -> chatClients.get("deepseek");
            case "chinese-text" -> chatClients.get("qwen");
            case "customer-service" -> chatClients.get("qwen");
            // 未知场景 fallback 到配置文件里的 default-provider
            default -> chatClients.getOrDefault(defaultProvider, chatClients.get("deepseek"));
        };
    }

    /**
     * 带降级的调用：优先用 preferredProvider，失败后自动切换到另一个。
     * 模型规格（model 名）从 app.ai.providers 配置里读取，改配置不改代码即可切换。
     */
    public String callWithFallback(String message, String preferredProvider) {
        // 找不到指定厂商时，退回到配置的 defaultProvider
        ChatClient primary = chatClients.getOrDefault(preferredProvider,
                chatClients.get(defaultProvider));

        // 从 providers 配置里读 model 名，动态覆盖 ChatClient 默认的模型规格
        // 例：app.ai.providers.deepseek.model=deepseek-reasoner 即可切到推理模式
        // 如果配置里没有这个 provider 的条目，options 为 null，沿用 ChatClient 默认值
        ChatOptions options = Optional.ofNullable(properties.getProviders().get(preferredProvider))
                .map(cfg -> (ChatOptions) ChatOptions.builder().model(cfg.model()).build())
                .orElse(null);

        try {
            var prompt = primary.prompt().user(message);
            if (options != null) prompt = prompt.options(options); // 有配置就覆盖，没有就不传
            return prompt.call().content();
        } catch (Exception primaryException) {
            // 主模型调用失败（超时、限流、服务不可用等），自动切换到 Map 里的另一个 ChatClient
            ChatClient fallback = chatClients.values().stream()
                    .filter(c -> c != primary)  // 排除刚才失败的那个
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("所有模型均不可用", primaryException));

            // 降级调用不传 options，用备用模型的默认配置
            return fallback.prompt()
                    .user(message)
                    .call()
                    .content();
        }
    }
}