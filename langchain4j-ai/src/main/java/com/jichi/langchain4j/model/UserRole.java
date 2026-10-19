package com.jichi.langchain4j.model;

// 用户角色，对应不同工具集
public enum UserRole {
    GUEST,   // 访客：只读
    MEMBER,  // 会员：读+部分写
    ADMIN    // 管理员：全权限
}