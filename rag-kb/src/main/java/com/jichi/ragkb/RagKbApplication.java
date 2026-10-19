package com.jichi.ragkb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync        // 开启异步，支持索引任务异步执行
@EnableRetry        // 开启重试，支持 Embedding 调用失败重试
@EnableCaching
public class RagKbApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagKbApplication.class, args);
    }

}