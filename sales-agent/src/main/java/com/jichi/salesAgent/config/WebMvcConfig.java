package com.jichi.salesAgent.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.jichi.salesAgent.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.auth.enabled:true}")
    private boolean authEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!authEnabled) {
            log.warn(">>> 权限校验已关闭（app.auth.enabled=false），仅限开发测试使用 <<<");
            // 只注册 ThreadLocal 清理拦截器，不注册 Sa-Token 登录校验
            registry.addInterceptor(new HandlerInterceptor() {
                @Override
                public void afterCompletion(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Object handler, Exception ex) {
                    UserContext.clear();
                }
            }).addPathPatterns("/**");
            return;
        }

        // 以下是正常的权限拦截器逻辑（auth.enabled=true 时生效）
        registry.addInterceptor(
                        new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns("/auth/login", "/actuator/**", "/static/**");

        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) {
                if (StpUtil.isLogin()) {
                    Long userId = StpUtil.getLoginIdAsLong();
                    SaSession session = StpUtil.getSession();
                    String username = (String) session.get("username");
                    String role = (String) session.get("role");
                    Long regionId = session.get("regionId") instanceof Number n ? n.longValue() : null;
                    Long repId = session.get("repId") instanceof Number n ? n.longValue() : null;
                    UserContext.set(new UserContext.UserInfo(userId, username, role, regionId, repId));
                    log.debug("用户已认证: userId={}, role={}", userId, role);
                }
                return true;
            }

            @Override
            public void afterCompletion(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Object handler, Exception ex) {
                UserContext.clear();
            }
        }).addPathPatterns("/**");
    }
}