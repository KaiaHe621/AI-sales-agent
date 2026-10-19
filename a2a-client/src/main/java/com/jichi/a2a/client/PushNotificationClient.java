package com.jichi.a2a.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class PushNotificationClient {

    private final RestTemplate restTemplate;

    public PushNotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 提交任务后，注册 Webhook 回调地址
     */
    public void registerWebhook(String agentUrl, String taskId,
                                String callbackUrl, String secret) {
        // Map.of() 不允许 null 值，secret 可选，单独构建 pushNotification
        Map<String, Object> pushNotification = new java.util.HashMap<>();
        pushNotification.put("url", callbackUrl);
        if (secret != null) {
            pushNotification.put("token", secret);
        }

        Map<String, Object> rpcRequest = Map.of(
                "jsonrpc", "2.0",
                "method", "tasks/pushNotification/set",
                "params", Map.of(
                        "id", taskId,
                        "pushNotification", pushNotification
                ),
                "id", UUID.randomUUID().toString()
        );

        restTemplate.postForEntity(agentUrl + "/",
                new org.springframework.http.HttpEntity<>(rpcRequest,
                        jsonHeaders()), Map.class);
    }

    private org.springframework.http.HttpHeaders jsonHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }
}