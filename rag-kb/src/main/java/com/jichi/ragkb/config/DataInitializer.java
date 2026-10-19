package com.jichi.ragkb.config;

import com.jichi.ragkb.entity.KbDocument;
import com.jichi.ragkb.repository.KbDocumentRepository;
import com.jichi.ragkb.service.IndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 开发环境数据初始化器
 * 把 src/test/resources/test-docs/ 下的样本文档插入数据库并触发索引。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final KbDocumentRepository documentRepository;
    private final IndexService indexService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (documentRepository.count() > 0) {
            log.info("[DataInit] 已有文档数据，跳过初始化");
            return;
        }

        log.info("[DataInit] 开始初始化测试文档...");

        initDocument(1L, "hr-handbook.txt", "employee-handbook.txt",
                "TXT", 1L, "test-docs/hr-handbook.txt");
        initDocument(2L, "tech-spec.txt", "tech-specification.txt",
                "TXT", 2L, "test-docs/tech-spec.txt");
        initDocument(3L, "product-faq.txt", "product-faq.txt",
                "TXT", 3L, "test-docs/product-faq.txt");

        log.info("[DataInit] 测试文档初始化完成，等待异步索引...");
    }

    private void initDocument(Long kbId, String minioPath, String fileName,
                              String fileType, Long uploadedBy,
                              String classpath) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpath);
        byte[] content = resource.getInputStream().readAllBytes();

        KbDocument doc = new KbDocument();
        doc.setKbId(kbId);
        doc.setFileName(fileName);
        doc.setFileType(fileType);
        doc.setFileSize((long) content.length);
        doc.setMinioPath(minioPath);
        doc.setUploadedBy(uploadedBy);
        KbDocument saved = documentRepository.save(doc);

        String text = new String(content, StandardCharsets.UTF_8);
        indexService.submitIndexTask(saved.getId(), text);

        log.info("[DataInit] 文档已提交索引：id={}, fileName={}", saved.getId(), fileName);
    }
}