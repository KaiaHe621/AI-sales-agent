package com.jichi.eval.regression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.eval.model.EvalReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaselineStore {

    private final ObjectMapper objectMapper;

    private static final String BASELINE_DIR = "eval-baselines";

    /**
     * 保存当前评估结果为 Baseline
     *
     * @param name    基准名称，如 "v1.0-production"
     * @param report  评估报告
     */
    public void save(String name, EvalReport report) throws IOException {
        Path dir = Paths.get(BASELINE_DIR);
        Files.createDirectories(dir);

        String filename = name + ".json";
        File file = dir.resolve(filename).toFile();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, report);

        log.info("Baseline 已保存：{}", file.getAbsolutePath());
    }

    /**
     * 加载指定名称的 Baseline
     */
    public EvalReport load(String name) throws IOException {
        File file = Paths.get(BASELINE_DIR, name + ".json").toFile();
        if (!file.exists()) {
            throw new IllegalArgumentException("Baseline 不存在：" + name
                    + "，请先运行 /eval/baseline/save 保存");
        }
        return objectMapper.readValue(file, EvalReport.class);
    }

    /**
     * 自动以当前时间戳命名保存 Baseline（适合 CI 场景）
     */
    public String saveWithTimestamp(EvalReport report) throws IOException {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String name = "baseline-" + timestamp;
        save(name, report);
        return name;
    }
}