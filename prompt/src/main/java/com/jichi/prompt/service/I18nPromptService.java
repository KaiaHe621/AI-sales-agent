package com.jichi.prompt.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class I18nPromptService {

    public String loadPrompt(String promptName, Locale locale) {
        String path = String.format("prompts/%s/%s.st",
                locale.getLanguage(), promptName);

        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            resource = new ClassPathResource("prompts/zh/" + promptName + ".st");
        }

        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载 Prompt 失败：" + path, e);
        }
    }
}