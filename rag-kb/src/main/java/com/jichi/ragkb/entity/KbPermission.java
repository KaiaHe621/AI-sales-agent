package com.jichi.ragkb.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "kb_permission")
@Data
public class KbPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kb_id", nullable = false)
    private Long kbId;

    /** 授权主体类型：USER / DEPARTMENT */
    @Column(name = "subject_type", nullable = false, length = 20)
    private String subjectType;

    /** 授权主体 ID：用户 ID 或部门 ID */
    @Column(name = "subject_id", nullable = false, length = 50)
    private String subjectId;

    /** 权限级别：READ / WRITE / ADMIN */
    @Column(name = "permission", nullable = false, length = 20)
    private String permission;

    @Column(name = "granted_by")
    private Long grantedBy;

    @CreationTimestamp
    @Column(name = "granted_at")
    private LocalDateTime grantedAt;
}