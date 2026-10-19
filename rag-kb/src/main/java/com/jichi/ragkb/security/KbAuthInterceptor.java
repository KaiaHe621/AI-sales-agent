package com.jichi.ragkb.security;

import cn.dev33.satoken.stp.StpUtil;
import com.jichi.ragkb.repository.KbPermissionRepository;
import com.jichi.ragkb.repository.KnowledgeBaseRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 知识库权限拦截器。
 * 在请求进入 Controller 前，把用户信息写入 UserContext。
 * 注意：具体的知识库权限校验在 PermissionService 中执行（Controller 调用），
 * 这个拦截器只负责初始化 UserContext。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KbAuthInterceptor implements HandlerInterceptor {

    private final KnowledgeBaseRepository kbRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {
        if (!StpUtil.isLogin()) {
            return true;  // 未登录的请求让 Sa-Token Filter 处理
        }

        // 从 Sa-Token Session 获取用户信息，写入 ThreadLocal
        // 注意：登录时用 StpUtil.getSession().set() 存储，这里也要用 getSession().get() 读取
        String userId = String.valueOf(StpUtil.getLoginId());
        Object deptObj = StpUtil.getSession().get("departmentId");
        Object roleObj = StpUtil.getSession().get("role");

        String deptId = deptObj != null ? deptObj.toString() : "";
        String role = roleObj != null ? roleObj.toString() : "MEMBER";

        UserContext.set(Long.parseLong(userId), deptId, role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        UserContext.clear();  // 请求结束后清理，防止内存泄漏
    }
}