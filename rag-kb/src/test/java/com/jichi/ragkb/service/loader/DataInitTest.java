package com.jichi.ragkb.service.loader;

import com.jichi.ragkb.entity.KbDocument;
import com.jichi.ragkb.repository.DocChunkRepository;
import com.jichi.ragkb.repository.KbDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class DataInitTest {

    @Autowired
    private KbDocumentRepository documentRepository;

    @Autowired
    private DocChunkRepository chunkRepository;

    @Test
    void verifyTestDataLoaded() {
        long docCount = documentRepository.countByStatus(KbDocument.DocumentStatus.DONE);
        long chunkCount = chunkRepository.count();

        assertThat(docCount).isGreaterThanOrEqualTo(3);
        assertThat(chunkCount).isGreaterThan(10);

        System.out.printf("已索引文档数：%d，分块总数：%d%n", docCount, chunkCount);
    }
}