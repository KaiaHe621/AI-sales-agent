package com.jichi.ragkb.service;

import com.jichi.ragkb.entity.*;
import com.jichi.ragkb.repository.*;
import com.jichi.ragkb.security.UserContext;
import com.jichi.ragkb.service.loader.ParseResult;
import com.jichi.ragkb.service.splitter.ChunkResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexService {

    private final KbDocumentRepository documentRepository;
    private final DocChunkRepository chunkRepository;
    private final IndexTaskRepository taskRepository;
    private final DocumentLoaderService loaderService;
    private final ChunkService chunkService;
    private final EmbeddingService embeddingService;
    private final MinioStorageService minioStorageService;
    private final IndexTaskLauncher taskLauncher;   // 后面会讲为什么要抽出这个类

    /**
     * 提交索引任务（支持直接传入文本，测试时跳过 MinIO）。
     */
    public void submitIndexTask(Long docId, String textContent) {
        IndexTask task = new IndexTask();
        task.setDocId(docId);
        task.setTaskType("INDEX");
        taskRepository.save(task);

        // 捕获当前用户上下文，传递给异步线程（ThreadLocal 不能跨线程）
        taskLauncher.launchWithText(task.getId(), docId, textContent,
                UserContext.getUserId(), UserContext.getDepartmentId(), UserContext.getRole());
    }

    /**
     * 提交索引任务（生产模式，从 MinIO 读取文件）。
     */
    public void submitIndexTask(Long docId) {
        IndexTask task = new IndexTask();
        task.setDocId(docId);
        task.setTaskType("INDEX");
        taskRepository.save(task);

        taskLauncher.launchFromMinio(task.getId(), docId,
                UserContext.getUserId(), UserContext.getDepartmentId(), UserContext.getRole());
    }

    /**
     * 从 MinIO 读取文件并执行索引（由 IndexTaskLauncher 异步调用）。
     */
    public void executeFromMinio(Long taskId, Long docId) {
        KbDocument doc = documentRepository.findById(docId).orElseThrow();
        try {
            byte[] fileBytes = minioStorageService.download(doc.getMinioPath());
            ParseResult parseResult = loaderService.load(
                    new ByteArrayInputStream(fileBytes), doc.getFileName());
            doIndex(taskId, docId, doc, parseResult);
        } catch (Exception e) {
            markFailed(taskId, docId, "从MinIO读取文件失败：" + e.getMessage());
        }
    }

    /**
     * 执行索引（直接使用文本内容，由 IndexTaskLauncher 异步调用）。
     */
    public void executeWithText(Long taskId, Long docId, String textContent) {
        KbDocument doc = documentRepository.findById(docId).orElseThrow();
        ParseResult parseResult = ParseResult.builder()
                .success(true)
                .pages(List.of(ParseResult.PageContent.builder()
                        .pageNum(1)
                        .text(textContent)
                        .build()))
                .totalPages(1)
                .build();
        doIndex(taskId, docId, doc, parseResult);
    }

    /**
     * 核心索引逻辑：解析 → 分块 → Embedding → 写库。
     *
     * 设计思路：整个管道是"先算后删再写"的顺序，而不是"先删再算再写"。
     * 这样设计是为了保证可用性——如果 Embedding 阶段调 API 失败了，
     * 旧版本的分块还在，用户查询不受影响。
     * 只有当新向量算完、确定能写入了，才去删旧数据。
     */
    private void doIndex(Long taskId, Long docId, KbDocument doc, ParseResult parseResult) {
        updateTaskStatus(taskId, IndexTask.TaskStatus.RUNNING);
        updateDocStatus(docId, KbDocument.DocumentStatus.PROCESSING);

        try {
            if (!parseResult.isSuccess()) {
                throw new RuntimeException("文档解析失败：" + parseResult.getErrorMsg());
            }

            // 第一步：分块
            List<ChunkResult> chunks = chunkService.chunk(parseResult);
            if (chunks.isEmpty()) {
                throw new RuntimeException("分块结果为空，文档可能无有效文本内容");
            }
            log.info("[IndexService] docId={}，分块完成，共{}块", docId, chunks.size());

            // 第二步：批量 Embedding
            List<String> texts = chunks.stream().map(ChunkResult::getContent).toList();
            List<float[]> embeddings = embeddingService.embedBatch(texts);

            // 第三步：删除旧版本分块（放在 Embedding 成功之后，保证有新数据才删旧数据）
            chunkRepository.deleteByDocIdAndDocVersionLessThan(docId, doc.getVersion());

            // 第四步：批量写入数据库
            List<DocChunk> docChunks = new ArrayList<>();
            int totalTokens = 0;
            for (int i = 0; i < chunks.size(); i++) {
                ChunkResult chunk = chunks.get(i);
                DocChunk docChunk = new DocChunk();
                docChunk.setDocId(docId);
                docChunk.setKbId(doc.getKbId());
                docChunk.setChunkIndex(chunk.getChunkIndex());
                docChunk.setContent(chunk.getContent());
                docChunk.setEmbedding(embeddings.get(i));
                docChunk.setPageNum(chunk.getPageNum());
                docChunk.setSectionTitle(chunk.getSectionTitle());
                docChunk.setTokenCount(chunk.getEstimatedTokens());
                docChunk.setDocVersion(doc.getVersion());
                docChunks.add(docChunk);
                totalTokens += chunk.getEstimatedTokens();
            }

            batchInsertChunks(docChunks);

            // 第五步：更新文档状态
            doc.setStatus(KbDocument.DocumentStatus.DONE);
            doc.setChunkCount(chunks.size());
            doc.setTokenCount(totalTokens);
            doc.setIndexedAt(LocalDateTime.now());
            documentRepository.save(doc);

            updateTaskStatus(taskId, IndexTask.TaskStatus.DONE);

            log.info("[IndexService] 索引完成：docId={}，chunks={}，tokens={}",
                    docId, chunks.size(), totalTokens);

        } catch (Exception e) {
            log.error("[IndexService] 索引失败：docId={}，error={}", docId, e.getMessage(), e);
            markFailed(taskId, docId, e.getMessage());
            retryIfPossible(taskId, docId);
        }
    }

    /**
     * 分批写入，每批 50 条。
     * 为什么不直接 saveAll 一把梭？因为一份大文档可能有几百个 chunk，
     * 单次 INSERT 几百行对数据库的压力很大（长事务 + 大量 WAL 日志），
     * 分批写可以减少单次事务大小，也方便观察写入进度。
     */
    private void batchInsertChunks(List<DocChunk> chunks) {
        int batchSize = 50;
        for (int i = 0; i < chunks.size(); i += batchSize) {
            List<DocChunk> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
            chunkRepository.saveAll(batch);
            log.debug("[IndexService] 写入批次 {}/{}",
                    i / batchSize + 1, (chunks.size() + batchSize - 1) / batchSize);
        }
    }

    private void markFailed(Long taskId, Long docId, String errorMsg) {
        IndexTask task = taskRepository.findById(taskId).orElseThrow();
        task.setStatus(IndexTask.TaskStatus.FAILED);
        task.setErrorMsg(errorMsg);
        task.setFinishedAt(LocalDateTime.now());
        taskRepository.save(task);

        documentRepository.findById(docId).ifPresent(doc -> {
            doc.setStatus(KbDocument.DocumentStatus.FAILED);
            doc.setErrorMsg(errorMsg);
            documentRepository.save(doc);
        });
    }

    private void retryIfPossible(Long taskId, Long docId) {
        IndexTask task = taskRepository.findById(taskId).orElseThrow();
        if (task.canRetry()) {
            task.setRetryCount(task.getRetryCount() + 1);
            task.setStatus(IndexTask.TaskStatus.PENDING);
            taskRepository.save(task);
            log.info("[IndexService] 任务将重试：taskId={}，retryCount={}", taskId, task.getRetryCount());
            // 延迟重试（指数退避：1s, 2s, 4s）
            scheduleRetry(taskId, docId, task.getRetryCount());
        }
    }

    /**
     * 延迟重试（指数退避）。
     * 这里直接用 Thread.sleep 而不是 ScheduledExecutorService，
     * 因为当前已经在 indexTaskExecutor 的异步线程里了，sleep 不会阻塞主线程。
     * 用 ScheduledExecutorService 反而要引入额外的线程池管理，过度设计了。
     */
    protected void scheduleRetry(Long taskId, Long docId, int retryCount) {
        try {
            long delay = (long) Math.pow(2, retryCount - 1) * 1000;
            Thread.sleep(delay);
            executeFromMinio(taskId, docId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void updateTaskStatus(Long taskId, IndexTask.TaskStatus status) {
        taskRepository.findById(taskId).ifPresent(t -> {
            t.setStatus(status);
            if (status == IndexTask.TaskStatus.RUNNING) t.setStartedAt(LocalDateTime.now());
            if (status == IndexTask.TaskStatus.DONE)    t.setFinishedAt(LocalDateTime.now());
            taskRepository.save(t);
        });
    }

    private void updateDocStatus(Long docId, KbDocument.DocumentStatus status) {
        documentRepository.findById(docId).ifPresent(d -> {
            d.setStatus(status);
            documentRepository.save(d);
        });
    }
}