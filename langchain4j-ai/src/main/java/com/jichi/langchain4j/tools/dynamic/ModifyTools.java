package com.jichi.langchain4j.tools.dynamic;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

/** 会员及以上角色才有的修改工具 */
@Component
public class ModifyTools {

    @Tool("申请订单退款")
    public String applyRefund(
            @P("订单号") String orderId,
            @P("退款原因") String reason) {
        return String.format("退款申请已提交：订单%s，原因：%s，预计1-3个工作日原路退回", orderId, reason);
    }

    @Tool("修改收货地址")
    public String updateAddress(
            @P("订单号") String orderId,
            @P("新地址") String newAddress) {
        return String.format("订单%s收货地址已更新为：%s", orderId, newAddress);
    }
}