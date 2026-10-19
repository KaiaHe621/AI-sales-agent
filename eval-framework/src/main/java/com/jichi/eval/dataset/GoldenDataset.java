package com.jichi.eval.dataset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jichi.eval.model.EvalCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoldenDataset {

    private final ObjectMapper objectMapper;

    public List<EvalCase> load() throws IOException {
        ClassPathResource resource = new ClassPathResource("dataset/golden.json");
        List<EvalCase> cases = objectMapper.readValue(
                resource.getInputStream(),
                new TypeReference<>() {
                }
        );
        log.info("加载 Golden Dataset，共 {} 条用例", cases.size());
        return cases;
    }

    public List<EvalCase> loadByTag(String tag) throws IOException {
        return load().stream()
                .filter(c -> tag.equals(c.getTag()))
                .toList();
    }
}