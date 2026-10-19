package com.jichi.langchain4j.tools.dynamic;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/** 仅管理员拥有的高权限工具 */
@Component
public class AdminTools {

    @Tool("强制关闭订单")
    public String forceCloseOrder(@P("订单号") String orderId) {
        return String.format("管理员操作：订单%s已强制关闭，相关款项将在3日内处理", orderId);
    }

    @Tool("查询用户账户信息")
    public String queryUserAccount(@P("用户ID") String userId) {
        return String.format("用户%s账户信息：余额128元，积分3200，风险等级：正常", userId);
    }
}