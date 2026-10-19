package com.jichi.ragkb.service;

import com.jichi.ragkb.dto.KnowledgeBaseCreateRequest;
import com.jichi.ragkb.dto.KnowledgeBaseVO;
import com.jichi.ragkb.entity.*;
import com.jichi.ragkb.repository.*;
import com.jichi.ragkb.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository kbRepository;
    private final KbPermissionRepository permissionRepository;
    private final KbDocumentRepository documentRepository;
    private final DocChunkRepository chunkRepository;
    private final IndexTaskRepository taskRepository;
    private final MinioStorageService minioService;
    private final IndexService indexService;

    @Transactional
    public KnowledgeBase create(KnowledgeBaseCreateRequest req) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(req.getName());
        kb.setDescription(req.getDescription());
        kb.setDepartmentId(req.getDepartmentId());
        kb.setIsPublic(req.getIsPublic());
        kb.setCreatedBy(UserContext.getUserId());

        KnowledgeBase saved = kbRepository.save(kb);

        // 创建者自动获得 ADMIN 权限
        KbPermission perm = new KbPermission();
        perm.setKbId(saved.getId());
        perm.setSubjectType("USER");
        perm.setSubjectId(String.valueOf(UserContext.getUserId()));
        perm.setPermission("ADMIN");
        perm.setGrantedBy(UserContext.getUserId());
        permissionRepository.save(perm);

        log.info("[KB] 知识库创建：id={}，name={}，creator={}", saved.getId(), saved.getName(), UserContext.getUserId());
        return saved;
    }

    /**
     * 查询当前用户可访问的知识库列表，并附带权限级别。
     */
    public List<KnowledgeBaseVO> listAccessible() {
        String dept = UserContext.getDepartmentId();
        String role = UserContext.getRole();
        String userId = String.valueOf(UserContext.getUserId());

        List<KnowledgeBase> kbList;
        if ("ADMIN".equalsIgnoreCase(role)) {
            kbList = kbRepository.findByIsDeletedFalse();
            return kbList.stream().map(kb -> toVO(kb, "ADMIN")).toList();
        }

        // 收集用户/部门的权限映射：kbId -> 最高权限
        Map<Long, String> permMap = new HashMap<>();

        permissionRepository.findBySubjectTypeAndSubjectId("DEPARTMENT", dept)
                .forEach(p -> permMap.merge(p.getKbId(), p.getPermission(), this::higherPermission));

        permissionRepository.findBySubjectTypeAndSubjectId("USER", userId)
                .forEach(p -> permMap.merge(p.getKbId(), p.getPermission(), this::higherPermission));

        // 公开库：没有显式权限的给 READ
        Set<Long> accessibleIds = new HashSet<>(permMap.keySet());
        kbRepository.findByIsPublicTrueAndIsDeletedFalse().forEach(kb -> {
            accessibleIds.add(kb.getId());
            permMap.putIfAbsent(kb.getId(), "READ");
        });

        if (accessibleIds.isEmpty()) return List.of();

        return kbRepository.findAllById(accessibleIds).stream()
                .filter(kb -> !kb.getIsDeleted())
                .map(kb -> toVO(kb, permMap.getOrDefault(kb.getId(), "READ")))
                .toList();
    }

    private KnowledgeBaseVO toVO(KnowledgeBase kb, String permission) {
        return KnowledgeBaseVO.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .departmentId(kb.getDepartmentId())
                .isPublic(kb.getIsPublic())
                .createdBy(kb.getCreatedBy())
                .createdAt(kb.getCreatedAt())
                .permission(permission)
                .build();
    }

    private static final Map<String, Integer> PERM_LEVEL = Map.of(
            "READ", 1, "WRITE", 2, "ADMIN", 3);

    private String higherPermission(String a, String b) {
        return PERM_LEVEL.getOrDefault(a, 0) >= PERM_LEVEL.getOrDefault(b, 0) ? a : b;
    }

    /**
     * 上传文档：存 MinIO → 创建文档记录 → 提交索引任务。
     * 返回文档 ID，前端可轮询状态。
     */
    @Transactional
    public KbDocument uploadDocument(Long kbId, org.springframework.web.multipart.MultipartFile file) {
        // 校验文件类型
        String fileName = file.getOriginalFilename();
        validateFileType(fileName);

        // 上传到 MinIO
        String minioPath = minioService.upload(kbId, file);

        // 创建文档记录
        KbDocument doc = new KbDocument();
        doc.setKbId(kbId);
        doc.setFileName(fileName);
        doc.setFileType(detectFileType(fileName));
        doc.setFileSize(file.getSize());
        doc.setMinioPath(minioPath);
        doc.setUploadedBy(UserContext.getUserId());
        KbDocument saved = documentRepository.save(doc);

        // 异步提交索引任务
        indexService.submitIndexTask(saved.getId());

        log.info("[KB] 文档上传：docId={}，fileName={}，kbId={}", saved.getId(), fileName, kbId);
        return saved;
    }

    /**
     * 删除文档：软删除文档记录 + 硬删除向量数据 + 删除 MinIO 文件。
     */
    @Transactional
    public void deleteDocument(Long docId) {
        KbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在：" + docId));

        // 软删除文档记录
        doc.setIsDeleted(true);
        documentRepository.save(doc);

        // 硬删除向量数据（向量数据不做软删除，占空间且不需要恢复）
        chunkRepository.deleteByDocId(docId);

        // 异步删除 MinIO 文件（不影响主流程）
        minioService.delete(doc.getMinioPath());

        log.info("[KB] 文档删除：docId={}，fileName={}", docId, doc.getFileName());
    }

    /**
     * 重建索引：递增版本号 → 提交新索引任务 → 新任务完成后旧分块自动清理。
     */
    @Transactional
    public void reindex(Long docId) {
        KbDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new RuntimeException("文档不存在：" + docId));

        // 版本号递增，索引完成后旧版本分块会被清理
        doc.setVersion(doc.getVersion() + 1);
        doc.setStatus(KbDocument.DocumentStatus.PENDING);
        doc.setErrorMsg(null);
        documentRepository.save(doc);

        indexService.submitIndexTask(docId);
        log.info("[KB] 触发重建索引：docId={}，newVersion={}", docId, doc.getVersion());
    }

    private void validateFileType(String fileName) {
        if (fileName == null) throw new RuntimeException("文件名不能为空");
        String lower = fileName.toLowerCase();
        if (!lower.endsWith(".pdf") && !lower.endsWith(".docx") &&
            !lower.endsWith(".md")  && !lower.endsWith(".txt")) {
            throw new RuntimeException("不支持的文件类型，目前支持：PDF、DOCX、MD、TXT");
        }
    }

    private String detectFileType(String fileName) {
        if (fileName == null) return "UNKNOWN";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf"))  return "PDF";
        if (lower.endsWith(".docx")) return "DOCX";
        if (lower.endsWith(".md"))   return "MD";
        if (lower.endsWith(".txt"))  return "TXT";
        return "UNKNOWN";
    }
}