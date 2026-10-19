package com.jichi.ragkb.service;

import com.jichi.ragkb.entity.KbDocument;
import com.jichi.ragkb.repository.KbDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档更新服务，处理"替换文档"场景：
 * 重新上传新版本文件，替换旧版本，触发重建索引。
 * <p>
 * 关键设计：
 * 1. 数据库更新和异步索引分开——先提交事务，再触发异步任务
 * 2. doc_version 递增，查询时按最新版本的 chunk 检索
 * 3. 新版本索引期间，旧版本 chunk 仍然可以被检索（不中断）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentUpdateService {

    private final KbDocumentRepository documentRepository;
    private final MinioStorageService minioService;
    private final IndexService indexService;

    /**
     * 第一步：更新数据库记录 + 上传新文件（事务内）。
     * 返回旧文件路径，供第二步删除。
     */
    @Transactional
    public String updateDocumentRecord(Long docId, MultipartFile newFile) {
        KbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在：" + docId));

        String oldMinioPath = doc.getMinioPath();

        String newMinioPath = minioService.upload(doc.getKbId(), newFile);

        doc.setFileName(newFile.getOriginalFilename());
        doc.setFileSize(newFile.getSize());
        doc.setMinioPath(newMinioPath);
        doc.setVersion(doc.getVersion() + 1);
        doc.setStatus(KbDocument.DocumentStatus.PENDING);
        doc.setErrorMsg(null);
        documentRepository.save(doc);

        log.info("[DocumentUpdate] 文档记录更新：docId={}，newVersion={}，newFile={}",
                docId, doc.getVersion(), newFile.getOriginalFilename());
        return oldMinioPath;
    }

    /**
     * 替换文档内容，保持文档 ID 不变。
     * 注意：不加 @Transactional，让 updateDocumentRecord 的事务先提交，
     * 再触发异步索引，确保异步线程读到的是最新的 minio_path。
     */
    public KbDocument replaceDocument(Long docId, MultipartFile newFile) {
        String oldMinioPath = updateDocumentRecord(docId, newFile);

        indexService.submitIndexTask(docId);

        minioService.delete(oldMinioPath);

        return documentRepository.findById(docId).orElseThrow();
    }

    /**
     * 强制重建索引（文档内容未变，但解析参数或分块策略更新了）。
     */
    @Transactional
    public void forceReindex(Long docId) {
        KbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在：" + docId));

        doc.setVersion(doc.getVersion() + 1);
        doc.setStatus(KbDocument.DocumentStatus.PENDING);
        doc.setErrorMsg(null);
        documentRepository.save(doc);
    }

    /**
     * forceReindex 的外层调用，事务提交后再触发异步索引。
     */
    public void forceReindexAndSubmit(Long docId) {
        forceReindex(docId);
        indexService.submitIndexTask(docId);
        log.info("[DocumentUpdate] 强制重建索引已提交：docId={}", docId);
    }
}