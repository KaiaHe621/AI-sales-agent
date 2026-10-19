package com.jichi.ragkb.service;

import com.jichi.ragkb.entity.KnowledgeBase;
import com.jichi.ragkb.exception.BizException;
import com.jichi.ragkb.repository.KbPermissionRepository;
import com.jichi.ragkb.repository.KnowledgeBaseRepository;
import com.jichi.ragkb.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final KbPermissionRepository permissionRepository;
    private final KnowledgeBaseRepository kbRepository;

    /** 检查当前用户对知识库是否有读权限 */
    public void requireRead(Long kbId) {
        if (UserContext.isAdmin()) return;  // 管理员直接放行

        // 知识库是公开的——注意不能用 ifPresent + return，lambda 里的 return 只退出 lambda 本身
        boolean isPublic = kbRepository.findById(kbId)
                .map(KnowledgeBase::getIsPublic)
                .orElse(false);
        if (isPublic) return;

        // 检查用户或部门权限
        String userId = String.valueOf(UserContext.getUserId());
        String deptId = UserContext.getDepartmentId();

        boolean hasPermission = permissionRepository.existsByKbIdAndSubjectTypeAndSubjectId(
                kbId, "USER", userId)
                || permissionRepository.existsByKbIdAndSubjectTypeAndSubjectId(
                kbId, "DEPARTMENT", deptId);

        if (!hasPermission) {
            throw BizException.forbidden("无权访问该知识库");
        }
    }

    /** 检查当前用户对知识库是否有写权限 */
    public void requireWrite(Long kbId) {
        if (UserContext.isAdmin()) return;

        String userId = String.valueOf(UserContext.getUserId());
        String deptId = UserContext.getDepartmentId();

        boolean hasWritePermission = permissionRepository
                .existsByKbIdAndSubjectTypeAndSubjectIdAndPermissionIn(
                        kbId, "USER", userId, java.util.List.of("WRITE", "ADMIN"))
                || permissionRepository
                .existsByKbIdAndSubjectTypeAndSubjectIdAndPermissionIn(
                        kbId, "DEPARTMENT", deptId, java.util.List.of("WRITE", "ADMIN"));

        if (!hasWritePermission) {
            throw BizException.forbidden("无文档管理权限");
        }
    }
}