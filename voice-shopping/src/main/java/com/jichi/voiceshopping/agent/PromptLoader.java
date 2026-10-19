package com.jichi.voiceshopping.agent;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Component
public class PromptLoader {

    public String load(String path) {
        try {
            return Files.readString(new ClassPathResource(path).getFile().toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("加载 prompt 失败：" + path, e);
        }
    }
}