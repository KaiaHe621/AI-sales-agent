package com.jichi.voiceshopping.security;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Long id() {
        return StpUtil.getLoginIdAsLong();
    }

    public boolean isLogin() {
        return StpUtil.isLogin();
    }

    /**
     * 检查用户是否属于某商家（商家运营角色专用，不在用户视角用）
     */
    public boolean belongsToMerchant(Long merchantId) {
        Object boundMerchant = StpUtil.getSession().get("merchantId");
        return boundMerchant != null && boundMerchant.equals(merchantId);
    }
}