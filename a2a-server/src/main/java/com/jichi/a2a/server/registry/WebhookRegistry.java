package com.jichi.a2a.server.registry;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebhookRegistry {

    private final Map<String, WebhookConfig> webhooks = new ConcurrentHashMap<>();

    public void register(String taskId, String callbackUrl, String secret) {
        webhooks.put(taskId, new WebhookConfig(callbackUrl, secret));
    }

    public WebhookConfig getWebhook(String taskId) {
        return webhooks.get(taskId);
    }

    public void remove(String taskId) {
        webhooks.remove(taskId);
    }

    public record WebhookConfig(String callbackUrl, String secret) {}
}